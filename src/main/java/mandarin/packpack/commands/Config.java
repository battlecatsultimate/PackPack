package mandarin.packpack.commands;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfigButtonHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Config extends ConstraintCommand {
    private final ConfigHolder config;
    private final boolean forServer;

    public Config(ROLE role, int lang, IDHolder id, ConfigHolder config, boolean forServer) {
        super(role, lang, id);

        this.config = Objects.requireNonNullElseGet(config, ConfigHolder::new);

        this.forServer = forServer;
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
            case LangID.TH:
                locale = LangID.getStringByID("lang_th", lang);
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

        String compact;
        String comp;

        if(config.compact) {
            compact = LangID.getStringByID("data_true", lang);
            comp = LangID.getStringByID("config_comtrue", lang);
        } else {
            compact = LangID.getStringByID("data_false", lang);
            comp = LangID.getStringByID("config_comfalse", lang);
        }

        String builder = "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                "**" + LangID.getStringByID("config_default", lang).replace("_", ""+ config.defLevel) + "**\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", config.defLevel+"") + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                LangID.getStringByID("config_unitdesc", lang) + "\n\n" +
                "**" + LangID.getStringByID("config_compact", lang).replace("_", compact) + "**\n" +
                comp;

        List<SelectOption> languages = new ArrayList<>();

        if(!forServer) {
            if(config.lang == -1)
                languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(true));
            else
                languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1"));
        }

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

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(Emoji.fromCustom(EmojiStore.PREVIOUS)).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(Emoji.fromCustom(EmojiStore.NEXT)));

        Message msg = ch.sendMessage(builder)
                .setComponents(
                        ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()),
                        ActionRow.of(StringSelectMenu.create("defLevels").addOptions(levels).build()),
                        ActionRow.of(StringSelectMenu.create("extra").addOptions(extras).build()),
                        ActionRow.of(pages),
                        ActionRow.of(components)
                )
                .setMessageReference(getMessage(event))
                .mentionRepliedUser(false)
                .complete();

        Member m = getMember(event);

        if(m != null) {
            StaticStore.putHolder(m.getId(), new ConfigButtonHolder(msg, getMessage(event), config, holder, ch.getId(), m.getId(), forServer));
        }
    }
}
