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
import discord4j.discordjson.json.ComponentData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ConfigButtonHolder extends InteractionHolder<ComponentInteractionEvent> {
    private final Message msg;
    private final ConfigHolder config;
    private final ConfigHolder backup;
    private final IDHolder holder;

    private final String channelID;
    private final String memberID;

    public ConfigButtonHolder(Message msg, Message author, ConfigHolder config, IDHolder holder, String channelID, String memberID) {
        super(ComponentInteractionEvent.class);

        this.msg = msg;
        this.config = config;
        this.backup = config.clone();
        this.holder = holder;

        this.channelID = channelID;
        this.memberID = memberID;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), ConfigButtonHolder.this));

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(ComponentInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if (!ch.getId().asString().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember().get();

        if(!mem.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message m = event.getMessage().get();

        if(!m.getId().asString().equals(msg.getId().asString()))
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
            case "language":
                SelectMenuInteractionEvent es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                config.lang = StaticStore.safeParseInt(es.getValues().get(0));

                return getMono(event);
            case "defLevels":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                config.defLevel = StaticStore.safeParseInt(es.getValues().get(0));

                return getMono(event);
            case "extra":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                config.extra = es.getValues().get(0).equals("true");

                return getMono(event);
            case "unit":
                es = (SelectMenuInteractionEvent) event;

                if(es.getValues().size() != 1)
                    return Mono.empty();

                config.useFrame = es.getValues().get(0).equals("frame");

                return getMono(event);
            case "confirm":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                StaticStore.config.put(memberID, config);

                int lang = config.lang;

                if(lang == -1)
                    lang = holder.serverLocale;

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("config_apply", lang)))
                                .components(new ArrayList<>())
                                .build()
                ));
            case "cancel":
                expired = true;
                StaticStore.removeHolder(memberID, this);

                StaticStore.config.put(memberID, backup);

                return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                        WebhookMessageEditRequest.builder()
                                .content(wrap(LangID.getStringByID("config_cancel", backup.lang)))
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
            m.content(wrap(LangID.getStringByID("config_expire", config.lang)));
            m.components(new ArrayList<>());
        });
    }

    private Mono<MessageData> getMono(ComponentInteractionEvent event) {
        return event.deferEdit().then(event.getInteractionResponse().editInitialResponse(
                WebhookMessageEditRequest.builder()
                        .content(wrap(parseMessage()))
                        .components(parseComponents())
                        .build()
        ));
    }

    private List<ComponentData> parseComponents() {
        int lang = config.lang;

        if(lang == -1)
            lang = holder.serverLocale;

        List<ComponentData> m = new ArrayList<>();

        List<SelectMenu.Option> languages = new ArrayList<>();

        if(config.lang == -1)
            languages.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_auto", lang), "-1"));
        else
            languages.add(SelectMenu.Option.of(LangID.getStringByID("config_auto", lang), "-1"));

        for(int i = 0; i < StaticStore.langIndex.length; i++) {
            String l = LangID.getStringByID("lang_"+StaticStore.langCode[i], lang);

            if(config.lang == StaticStore.langIndex[i]) {
                languages.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]));
            } else {
                languages.add(SelectMenu.Option.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]));
            }
        }

        m.add(ActionRow.of(SelectMenu.of("language", languages)).getData());

        List<SelectMenu.Option> levels = new ArrayList<>();

        for(int i = 0; i <= 50; i += 5) {
            if(config.defLevel == i) {
                levels.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i));
            } else {
                levels.add(SelectMenu.Option.of(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i));
            }
        }

        m.add(ActionRow.of(SelectMenu.of("defLevels", levels)).getData());

        List<SelectMenu.Option> extras = new ArrayList<>();

        if(config.extra) {
            extras.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
            extras.add(SelectMenu.Option.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
        } else {
            extras.add(SelectMenu.Option.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
            extras.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
        }

        m.add(ActionRow.of(SelectMenu.of("extra", extras)).getData());

        List<SelectMenu.Option> units = new ArrayList<>();

        if(config.useFrame) {
            units.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
            units.add(SelectMenu.Option.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
        } else {
            units.add(SelectMenu.Option.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
            units.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
        }

        m.add(ActionRow.of(SelectMenu.of("unit", units)).getData());

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        m.add(ActionRow.of(components).getData());

        return m;
    }

    private String parseMessage() {
        int lang = config.lang;

        if(lang == -1)
            lang = holder.serverLocale;

        String locale;

        switch (config.lang) {
            case LangID.EN:
                locale = LangID.getStringByID("lang_en", lang);
                break;
            case LangID.JP:
                locale = LangID.getStringByID("lang_jp", lang);
                break;
            case LangID.KR:
                locale = LangID.getStringByID("lang_kr", lang);
                break;
            case LangID.ZH:
                locale = LangID.getStringByID("lang_zh", lang);
                break;
            case LangID.FR:
                locale = LangID.getStringByID("lang_fr", lang);
                break;
            case LangID.IT:
                locale = LangID.getStringByID("lang_it", lang);
                break;
            case LangID.ES:
                locale = LangID.getStringByID("lang_es", lang);
                break;
            case LangID.DE:
                locale = LangID.getStringByID("lang_de", lang);
                break;
            default:
                locale = LangID.getStringByID("config_auto", lang);
                break;
        }

        String ex;
        String bool;

        if(config.extra) {
            ex = LangID.getStringByID("config_extrue", lang);
            bool = LangID.getStringByID("data_true", lang);
        } else {
            ex = LangID.getStringByID("config_exfalse", lang);
            bool = LangID.getStringByID("data_false", lang);
        }

        String unit;

        if(config.useFrame)
            unit = LangID.getStringByID("config_frame", lang);
        else
            unit = LangID.getStringByID("config_second", lang);

        return "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                "**" + LangID.getStringByID("config_default", lang).replace("_", ""+config.defLevel) + "**\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", ""+config.defLevel) + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                LangID.getStringByID("config_unitdesc", lang);
    }
}
