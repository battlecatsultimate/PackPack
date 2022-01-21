package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.AllowedMentionsData;
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.ScamLinkHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ScamLinkSubscriptionHolder extends InteractionHolder<ComponentInteractionEvent> {
    private final Message msg;
    private final String channelID;
    private final String memberID;
    private final int lang;

    private final String targetChannel;
    private final String mute;

    private ScamLinkHandler.ACTION action = ScamLinkHandler.ACTION.MUTE;
    private boolean noticeAll = false;

    public ScamLinkSubscriptionHolder(Message msg, String channelID, String memberID, int lang, String targetChannel, String mute) {
        super(ComponentInteractionEvent.class);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = memberID;
        this.lang = lang;

        this.targetChannel = targetChannel;
        this.mute = mute;
    }

    @Override
    public int handleEvent(ComponentInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member m = event.getInteraction().getMember().get();

        if(!m.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message me = event.getMessage().get();

        if(!me.getId().asString().equals(msg.getId().asString()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public Mono<?> getInteraction(ComponentInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();
        Guild g = msg.getGuild().block();

        if(ch == null || g == null)
            return Mono.empty();

        switch (event.getCustomId()) {
            case "action":
                SelectMenuInteractionEvent es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

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

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(parseMessage()))
                                .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().build())))
                                .components(getComponents())
                                .build()
                ));
            case "notice":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                noticeAll = es.getValues().get(0).equals("noticeAll");

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(parseMessage()))
                                .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().build())))
                                .components(getComponents())
                                .build()
                ));
            case "confirm":
                if(action != ScamLinkHandler.ACTION.MUTE || mute != null) {
                    expired = true;
                    StaticStore.removeHolder(memberID, this);

                    ScamLinkHandler handler = new ScamLinkHandler(memberID, g.getId().asString(), targetChannel, mute, action, noticeAll);

                    StaticStore.scamLinkHandlers.servers.put(g.getId().asString(), handler);

                    Command.createMessage(ch, m -> {
                        m.messageReference(msg.getId());
                        m.allowedMentions(AllowedMentions.builder().build());
                        m.content(LangID.getStringByID("subscam_done", lang).replace("_", targetChannel));
                    });

                    return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                            WebhookMessageEditRequest.builder()
                                    .content(wrap(parseMessage()))
                                    .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().build())))
                                    .components(new ArrayList<>())
                                    .build()
                    ));
                } else {
                    return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                            WebhookMessageEditRequest.builder()
                                    .content(wrap(LangID.getStringByID("subscam_nomute", lang)))
                                    .allowedMentions(Possible.of(Optional.of(AllowedMentionsData.builder().build())))
                                    .components(new ArrayList<>())
                                    .build()
                    ));
                }
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("subscam_cancel", lang)))
                                .components(new ArrayList<>())
                                .build()
                ));
        }

        return Mono.empty();
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        Command.editMessage(msg, m -> {
            m.components(new ArrayList<>());
            m.content(wrap(LangID.getStringByID("subscam_expire", lang)));
        });
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

    private List<ComponentData> getComponents() {
        List<ComponentData> m = new ArrayList<>();

        List<SelectMenu.Option> options = new ArrayList<>();

        if(action == ScamLinkHandler.ACTION.MUTE) {
            options.add(SelectMenu.Option.ofDefault(LangID.getStringByID("mute", lang), "mute"));
        } else {
            options.add(SelectMenu.Option.of(LangID.getStringByID("mute", lang), "mute"));
        }

        if(action == ScamLinkHandler.ACTION.KICK) {
            options.add(SelectMenu.Option.ofDefault(LangID.getStringByID("kick", lang), "kick"));
        } else {
            options.add(SelectMenu.Option.of(LangID.getStringByID("kick", lang), "kick"));
        }

        if(action == ScamLinkHandler.ACTION.BAN) {
            options.add(SelectMenu.Option.ofDefault(LangID.getStringByID("ban", lang), "ban"));
        } else {
            options.add(SelectMenu.Option.of(LangID.getStringByID("ban", lang), "ban"));
        }

        m.add(ActionRow.of(SelectMenu.of("action", options)).getData());

        List<SelectMenu.Option> notices = new ArrayList<>();

        if(!noticeAll) {
            notices.add(SelectMenu.Option.ofDefault(LangID.getStringByID("noticex", lang), "noticeX"));
            notices.add(SelectMenu.Option.of(LangID.getStringByID("noticeall", lang), "noticeAll"));
        } else {
            notices.add(SelectMenu.Option.ofDefault(LangID.getStringByID("noticeall", lang), "noticeAll"));
            notices.add(SelectMenu.Option.of(LangID.getStringByID("noticex", lang), "noticeX"));
        }

        m.add(ActionRow.of(SelectMenu.of("notice", notices)).getData());

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        m.add(ActionRow.of(components).getData());

        return m;
    }
}
