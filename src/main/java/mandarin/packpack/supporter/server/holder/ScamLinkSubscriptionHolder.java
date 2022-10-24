package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.ArrayList;
import java.util.List;

public class ScamLinkSubscriptionHolder extends InteractionHolder<GenericComponentInteractionCreateEvent> {
    private final Message msg;
    private final String channelID;
    private final String memberID;
    private final int lang;

    private final String targetChannel;
    private final String mute;

    private ScamLinkHandler.ACTION action = ScamLinkHandler.ACTION.MUTE;
    private boolean noticeAll = false;

    public ScamLinkSubscriptionHolder(Message msg, Message author, String channelID, int lang, String targetChannel, String mute) {
        super(GenericComponentInteractionCreateEvent.class, author);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = author.getAuthor().getId();
        this.lang = lang;

        this.targetChannel = targetChannel;
        this.mute = mute;
    }

    @Override
    public int handleEvent(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();

        if(!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member m = event.getInteraction().getMember();

        if(!m.getId().equals(memberID))
            return RESULT_STILL;

        Message me = event.getMessage();

        if(!me.getId().equals(msg.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = msg.getChannel();
        Guild g = msg.getGuild();

        switch (event.getComponentId()) {
            case "action":
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                switch (es.getValues().get(0)) {
                    case "mute":
                        action = ScamLinkHandler.ACTION.MUTE;
                        break;
                    case "kick":
                        action = ScamLinkHandler.ACTION.KICK;
                        break;
                    case "ban":
                        action = ScamLinkHandler.ACTION.BAN;
                        break;
                }
                
                event.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(getComponents())
                        .queue();

                break;
            case "notice":
                es = (StringSelectInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return;

                noticeAll = es.getValues().get(0).equals("noticeAll");

                event.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(getComponents())
                        .queue();

                break;
            case "confirm":
                if(action != ScamLinkHandler.ACTION.MUTE || mute != null) {
                    expired = true;
                    StaticStore.removeHolder(memberID, this);

                    ScamLinkHandler handler = new ScamLinkHandler(memberID, g.getId(), targetChannel, mute, action, noticeAll);

                    StaticStore.scamLinkHandlers.servers.put(g.getId(), handler);

                    ch.sendMessage(LangID.getStringByID("subscam_done", lang).replace("_", targetChannel))
                            .setMessageReference(msg)
                            .setAllowedMentions(new ArrayList<>())
                            .queue();

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

                break;
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                event.deferEdit()
                        .setContent(LangID.getStringByID("subscam_cancel", lang))
                        .setComponents()
                        .queue();

                break;
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("subscam_expire", lang)).setAllowedMentions(new ArrayList<>()).queue();
    }

    private String parseMessage() {
        String result = LangID.getStringByID("subscam_before", lang) + "\n\n";

        switch (action) {
            case MUTE:
                result += LangID.getStringByID("subscam_actionmute", lang) + "\n\n";
                break;
            case KICK:
                result += LangID.getStringByID("subscam_actionkick", lang) + "\n\n";
                break;
            case BAN:
                result += LangID.getStringByID("subscam_actionban", lang) + "\n\n";
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
