package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ScamLinkSubscriptionHolder extends ComponentHolder {
    private final String targetChannel;
    private final String mute;

    private ScamLinkHandler.ACTION action = ScamLinkHandler.ACTION.MUTE;
    private boolean noticeAll = false;

    public ScamLinkSubscriptionHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, CommonStatic.Lang.Locale lang, String targetChannel, String mute) {
        super(author, userID, channelID, message, lang);

        this.targetChannel = targetChannel;
        this.mute = mute;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = message.getChannel();
        Guild g = message.getGuild();

        switch (event.getComponentId()) {
            case "action" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                switch (es.getValues().getFirst()) {
                    case "scamDetector.action.mute" -> action = ScamLinkHandler.ACTION.MUTE;
                    case "scamDetector.action.kick" -> action = ScamLinkHandler.ACTION.KICK;
                    case "scamDetector.action.ban" -> action = ScamLinkHandler.ACTION.BAN;
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
                
                noticeAll = es.getValues().getFirst().equals("noticeAll");
                
                event.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(getComponents())
                        .queue();
            }
            case "confirm" -> {
                if (action != ScamLinkHandler.ACTION.MUTE || mute != null) {
                    ScamLinkHandler handler = new ScamLinkHandler(userID, g.getId(), targetChannel, mute, action, noticeAll);

                    StaticStore.scamLinkHandlers.servers.put(g.getId(), handler);

                    Command.replyToMessageSafely(ch, LangID.getStringByID("subscribeScamDetector.done", lang).replace("_", targetChannel), message, a -> a);

                    event.deferEdit()
                            .setContent(parseMessage())
                            .setComponents()
                            .queue();

                    end(true);
                } else {
                    event.deferEdit()
                            .setContent(LangID.getStringByID("subscribeScamDetector.failed.noMuteRole", lang))
                            .setComponents()
                            .queue();
                }
            }
            case "cancel" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("subscribeScamDetector.canceled", lang))
                        .setComponents()
                        .queue();

                end(true);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("subscribeScamDetector.expired", lang)).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
    }

    private String parseMessage() {
        String result = LangID.getStringByID("subscribeScamDetector.decision.explanation", lang) + "\n\n";

        switch (action) {
            case MUTE -> result += LangID.getStringByID("subscribeScamDetector.decision.mute", lang) + "\n\n";
            case KICK -> result += LangID.getStringByID("subscribeScamDetector.decision.kick", lang) + "\n\n";
            case BAN -> result += LangID.getStringByID("subscribeScamDetector.decision.ban", lang) + "\n\n";
        }

        if(noticeAll) {
            result += LangID.getStringByID("subscribeScamDetector.notice.allUsers", lang);
        } else {
            result += LangID.getStringByID("subscribeScamDetector.notice.onlyMember", lang);
        }

        return result;
    }

    private List<ActionRow> getComponents() {
        List<ActionRow> m = new ArrayList<>();

        List<SelectOption> options = new ArrayList<>();

        if(action == ScamLinkHandler.ACTION.MUTE) {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.mute", lang), "scamDetector.action.mute").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.mute", lang), "scamDetector.action.mute"));
        }

        if(action == ScamLinkHandler.ACTION.KICK) {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.kick", lang), "scamDetector.action.kick").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.kick", lang), "scamDetector.action.kick"));
        }

        if(action == ScamLinkHandler.ACTION.BAN) {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.ban", lang), "scamDetector.action.ban").withDefault(true));
        } else {
            options.add(SelectOption.of(LangID.getStringByID("scamDetector.action.ban", lang), "scamDetector.action.ban"));
        }

        m.add(ActionRow.of(StringSelectMenu.create("action").addOptions(options).build()));

        List<SelectOption> notices = new ArrayList<>();

        if(!noticeAll) {
            notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.onlyMember", lang), "noticeX").withDefault(true));
            notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.allUsers", lang), "noticeAll"));
        } else {
            notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.allUsers", lang), "noticeAll").withDefault(true));
            notices.add(SelectOption.of(LangID.getStringByID("scamDetector.notice.onlyMember", lang), "noticeX"));
        }

        m.add(ActionRow.of(StringSelectMenu.create("notice").addOptions(notices).build()));

        List<Button> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        m.add(ActionRow.of(components));

        return m;
    }
}
