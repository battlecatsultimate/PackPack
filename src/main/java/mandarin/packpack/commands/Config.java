package mandarin.packpack.commands;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfigButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.ArrayList;
import java.util.List;

public class Config extends ConstraintCommand {
    private final ConfigHolder config;

    public Config(ROLE role, int lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id);

        this.config = config;
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

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

        List<SelectOption> languages = new ArrayList<>();

        if(config.lang == -1)
            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(true));
        else
            languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1"));

        for(int i = 0; i < StaticStore.langIndex.length; i++) {
            String l = LangID.getStringByID("lang_"+StaticStore.langCode[i], config.lang);

            if(config.lang == StaticStore.langIndex[i]) {
                languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]).withDefault(true));
            } else {
                languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), ""+StaticStore.langIndex[i]));
            }
        }

        List<SelectOption> levels = new ArrayList<>();

        for(int i = 0; i <= 50; i += 5) {
            if(config.defLevel == i) {
                levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i).withDefault(true));
            } else {
                levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", i == 0 ? "1" : ""+i), i == 0 ? "1" : ""+i));
            }
        }

        List<SelectOption> extras = new ArrayList<>();

        if(config.extra) {
            extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true").withDefault(true));
            extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false"));
        } else {
            extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang)), "true"));
            extras.add(SelectOption.of(LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang)), "false").withDefault(true));
        }

        List<SelectOption> units = new ArrayList<>();

        if(config.useFrame) {
            units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame").withDefault(true));
            units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second"));
        } else {
            units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_frame", lang)), "frame"));
            units.add(SelectOption.of(LangID.getStringByID("config_units", lang).replace("_", LangID.getStringByID("config_second", lang)), "second").withDefault(true));
        }

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        Message msg = ch.sendMessage(builder)
                .setActionRows(
                        ActionRow.of(SelectMenu.create("language").addOptions(languages).build()),
                        ActionRow.of(SelectMenu.create("defLevels").addOptions(levels).build()),
                        ActionRow.of(SelectMenu.create("extra").addOptions(extras).build()),
                        ActionRow.of(SelectMenu.create("unit").addOptions(units).build()),
                        ActionRow.of(components)
                ).complete();

        Member m = getMember(event);

        if(m != null) {
            StaticStore.putHolder(m.getId(), new ConfigButtonHolder(msg, getMessage(event), config, holder, ch.getId(), m.getId()));
        }
    }
}
