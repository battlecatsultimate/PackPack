package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScamLinkSubscriptionHolder extends ComponentHolder {
    private final Message msg;
    private final int lang;

    private final String targetChannel;
    private final String mute;

    private ScamLinkHandler.ACTION action = ScamLinkHandler.ACTION.MUTE;
    private boolean noticeAll = false;

    public ScamLinkSubscriptionHolder(Message author, Message msg, String channelID, int lang, String targetChannel, String mute) {
        super(author, channelID, msg.getId());

        this.msg = msg;
        this.lang = lang;

        this.targetChannel = targetChannel;
        this.mute = mute;
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();
        Guild g = msg.getGuild();

        switch (event.getComponentId()) {
            case "action" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                switch (es.getValues().get(0)) {
                    case "mute" -> action = ScamLinkHandler.ACTION.MUTE;
                    case "kick" -> action = ScamLinkHandler.ACTION.KICK;
                    case "ban" -> action = ScamLinkHandler.ACTION.BAN;
                }
                
                event.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(getComponents())
                        .queue();
            }
            case "notice" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                noticeAll = es.getValues().get(0).equals("noticeAll");
                
                event.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(getComponents())
                        .queue();
            }
            case "confirm" -> {
                if (action != ScamLinkHandler.ACTION.MUTE || mute != null) {
                    expired = true;
                    
                    StaticStore.removeHolder(userID, this);

                    ScamLinkHandler handler = new ScamLinkHandler(userID, g.getId(), targetChannel, mute, action, noticeAll);

                    StaticStore.scamLinkHandlers.servers.put(g.getId(), handler);

                    Command.replyToMessageSafely(ch, LangID.getStringByID("subscam_done", lang).replace("_", targetChannel), msg, a -> a);

                    event.deferEdit()
                            .setContent(parseMessage())
                            .setComponents()
                            .queue();
                } else {
                    event.deferEdit()
                            .setContent(LangID.getStringByID("subscam_nomute", lang))
                            .setComponents()
                            .queue();
                }
            }
            case "cancel" -> {
                expired = true;
                StaticStore.removeHolder(userID, this);
                event.deferEdit()
                        .setContent(LangID.getStringByID("subscam_cancel", lang))
                        .setComponents()
                        .queue();
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("subscam_expire", lang)).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
    }

    private String parseMessage() {
        String result = LangID.getStringByID("subscam_before", lang) + "\n\n";

        switch (action) {
            case MUTE -> result += LangID.getStringByID("subscam_actionmute", lang) + "\n\n";
            case KICK -> result += LangID.getStringByID("subscam_actionkick", lang) + "\n\n";
            case BAN -> result += LangID.getStringByID("subscam_actionban", lang) + "\n\n";
        }

        if(noticeAll) {
            result += LangID.getStringByID("subscam_noticeall", lang);
        } else {
            result += LangID.getStringByID("subscam_noticex", lang);
        }

        return result;
    }

    private List<ActionRow> getComponents() {
        List<ActionRow> m = new ArrayList<>();

        List<SelectOption> options = new ArrayList<>();

        if(action == ScamLinkHandler.ACTION.MUTE) {
            options.add(SelectOption.of(LangID.getStringByID("mute", lang), "mute").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("mute", lang), "mute"));
        }

        if(action == ScamLinkHandler.ACTION.KICK) {
            options.add(SelectOption.of(LangID.getStringByID("kick", lang), "kick").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("kick", lang), "kick"));
        }

        if(action == ScamLinkHandler.ACTION.BAN) {
            options.add(SelectOption.of(LangID.getStringByID("ban", lang), "ban").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("ban", lang), "ban"));
        }

        m.add(ActionRow.of(StringSelectMenu.create("action").addOptions(options).build()));

        List<SelectOption> notices = new ArrayList<>();

        if(!noticeAll) {
            notices.add(SelectOption.of(LangID.getStringByID("noticex", lang), "noticeX").withDefault(true));
            notices.add(SelectOption.of(LangID.getStringByID("noticeall", lang), "noticeAll"));
        } else {
            notices.add(SelectOption.of(LangID.getStringByID("noticeall", lang), "noticeAll").withDefault(true));
            notices.add(SelectOption.of(LangID.getStringByID("noticex", lang), "noticeX"));
        }

        m.add(ActionRow.of(StringSelectMenu.create("notice").addOptions(notices).build()));

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        m.add(ActionRow.of(components));

        return m;
    }
}
