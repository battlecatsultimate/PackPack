package mandarin.packpack.commands;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.ConfigButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Config extends ConstraintCommand {
    private final ConfigHolder config;
    private final boolean forServer;

    public Config(ROLE role, int lang, IDHolder id, ConfigHolder config, boolean forServer) {
        super(role, lang, id, forServer);

        this.config = Objects.requireNonNullElseGet(config, ConfigHolder::new);

        this.forServer = forServer;
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String locale = switch (config.lang) {
            case LangID.EN -> LangID.getStringByID("lang_en", lang);
            case LangID.JP -> LangID.getStringByID("lang_jp", lang);
            case LangID.KR -> LangID.getStringByID("lang_kr", lang);
            case LangID.ZH -> LangID.getStringByID("lang_zh", lang);
            case LangID.FR -> LangID.getStringByID("lang_fr", lang);
            case LangID.IT -> LangID.getStringByID("lang_it", lang);
            case LangID.ES -> LangID.getStringByID("lang_es", lang);
            case LangID.DE -> LangID.getStringByID("lang_de", lang);
            case LangID.TH -> LangID.getStringByID("lang_th", lang);
            default -> LangID.getStringByID("config_auto", lang);
        };

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

        String trueForm;
        String tr;

        if(config.trueForm) {
            trueForm = LangID.getStringByID("data_true", lang);
            tr = LangID.getStringByID("config_truetrue", lang);
        } else {
            trueForm = LangID.getStringByID("data_false", lang);
            tr = LangID.getStringByID("config_truefalse", lang);
        }

        String message = "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**\n\n" +
                "**" + LangID.getStringByID("config_default", lang).replace("_", String.valueOf(config.defLevel)) + "**\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", String.valueOf(config.defLevel)) + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_unit", lang).replace("_", unit) + "**\n" +
                LangID.getStringByID("config_unitdesc", lang) + "\n\n" +
                "**" + LangID.getStringByID("config_compact", lang).replace("_", compact) + "**\n" +
                comp + "\n\n" +
                "**" + String.format(LangID.getStringByID("config_trueform", lang), trueForm) + "**\n" +
                tr;

        if(forServer) {
            String force = LangID.getStringByID((holder != null && holder.forceCompact) ? "data_true" : "data_false", lang);
            String forc = LangID.getStringByID((holder != null && holder.forceCompact) ? "config_fortrue" : "config_forfalse", lang);

            message += "\n\n" +
                    "**" + LangID.getStringByID("config_force", lang).replace("_", force) + "**\n" +
                    forc;
        }

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
                languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), String.valueOf(StaticStore.langIndex[i])).withDefault(true));
            } else {
                languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), String.valueOf(StaticStore.langIndex[i])));
            }
        }

        List<SelectOption> levels = new ArrayList<>();

        for(int i = 0; i <= 50; i += 5) {
            final String level = i == 0 ? "1" : String.valueOf(i);

            if(config.defLevel == i) {
                levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", level), level).withDefault(true));
            } else {
                levels.add(SelectOption.of(LangID.getStringByID("config_default", lang).replace("_", level), level));
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

        Message msg = getRepliedMessageSafely(ch, message, getMessage(event), a -> a.setComponents(
                ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()),
                ActionRow.of(StringSelectMenu.create("defLevels").addOptions(levels).build()),
                ActionRow.of(StringSelectMenu.create("extra").addOptions(extras).build()),
                ActionRow.of(pages),
                ActionRow.of(components)
        ));

        Message original = getMessage(event);

        if(original == null)
            return;

        User u = original.getAuthor();

        StaticStore.putHolder(u.getId(), new ConfigButtonHolder(msg, original, config, holder, ch.getId(), u.getId(), forServer));
    }
}
