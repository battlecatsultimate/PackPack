package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfigButtonHolder;

import java.util.ArrayList;
import java.util.List;

public class Config extends ConstraintCommand {
    private final ConfigHolder config;

    public Config(ROLE role, int lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id);

        this.config = config;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Message msg = createMessage(ch, m -> {
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

            String builder = "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                    "**" + LangID.getStringByID("config_default", lang).replace("_", ""+ config.defLevel) + "**\n" +
                    LangID.getStringByID("config_deflvdesc", lang).replace("_", config.defLevel+"") + "\n\n" +
                    "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                    ex + "\n\n" +
                    "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                    LangID.getStringByID("config_unitdesc", lang);

            m.content(builder);

            List<SelectMenu.Option> languages = new ArrayList<>();

            if(config.lang == -1)
                languages.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_auto", lang), "-1"));
            else
                languages.add(SelectMenu.Option.of(LangID.getStringByID("config_auto", lang), "-1"));

            for(int i = 0; i < StaticStore.langIndex.length; i++) {
                String l = LangID.getStringByID("lang_"+StaticStore.langCode[i], config.lang);

                if(config.lang == StaticStore.langIndex[i]) {
                    languages.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]));
                } else {
                    languages.add(SelectMenu.Option.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]));
                }
            }

            m.addComponent(ActionRow.of(SelectMenu.of("language", languages)));

            List<SelectMenu.Option> levels = new ArrayList<>();

            for(int i = 0; i <= 50; i += 5) {
                if(config.defLevel == i) {
                    levels.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i));
                } else {
                    levels.add(SelectMenu.Option.of(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i));
                }
            }

            m.addComponent(ActionRow.of(SelectMenu.of("defLevels", levels)));

            List<SelectMenu.Option> extras = new ArrayList<>();

            if(config.extra) {
                extras.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                extras.add(SelectMenu.Option.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
            } else {
                extras.add(SelectMenu.Option.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
                extras.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
            }

            m.addComponent(ActionRow.of(SelectMenu.of("extra", extras)));

            List<SelectMenu.Option> units = new ArrayList<>();

            if(config.useFrame) {
                units.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
                units.add(SelectMenu.Option.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
            } else {
                units.add(SelectMenu.Option.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
                units.add(SelectMenu.Option.ofDefault(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
            }

            m.addComponent(ActionRow.of(SelectMenu.of("unit", units)));

            List<ActionComponent> components = new ArrayList<>();

            components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
            components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

            m.addComponent(ActionRow.of(components));
        });

        getMember(event).ifPresent(mem -> StaticStore.putHolder(mem.getId().asString(), new ConfigButtonHolder(msg, getMessage(event), config, holder, ch.getId().asString(), mem.getId().asString())));
    }
}
