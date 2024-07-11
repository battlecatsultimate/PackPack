package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfigButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Config extends ConstraintCommand {
    private final ConfigHolder config;

    public Config(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, ConfigHolder config) {
        super(role, lang, id, false);

        this.config = Objects.requireNonNullElseGet(config, ConfigHolder::new);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        CommonStatic.Lang.Locale language = config.lang;

        String locale;

        if (language == null) {
            locale = LangID.getStringByID("config_auto", lang);
        } else {
            locale = switch (language) {
                case EN -> LangID.getStringByID("lang_en", lang);
                case JP -> LangID.getStringByID("lang_jp", lang);
                case KR -> LangID.getStringByID("lang_kr", lang);
                case ZH -> LangID.getStringByID("lang_zh", lang);
                case FR -> LangID.getStringByID("lang_fr", lang);
                case IT -> LangID.getStringByID("lang_it", lang);
                case ES -> LangID.getStringByID("lang_es", lang);
                case DE -> LangID.getStringByID("lang_de", lang);
                case TH -> LangID.getStringByID("lang_th", lang);
                default -> LangID.getStringByID("config_auto", lang);
            };
        }

        String ex = LangID.getStringByID(config.extra ? "config_extrue" : "config_exfalse", lang);
        String bool = LangID.getStringByID(config.extra ? "data_true" : "data_false", lang);

        String message = "**" + LangID.getStringByID("config_default", lang).replace("_", String.valueOf(config.defLevel)) + "**\n\n" +
                LangID.getStringByID("config_deflvdesc", lang).replace("_", String.valueOf(config.defLevel)) + "\n\n" +
                "**" + LangID.getStringByID("config_extra", lang).replace("_", bool) + "**\n\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config_locale", lang).replace("_", locale) + "**";

        List<SelectOption> languages = new ArrayList<>();

        languages.add(SelectOption.of(LangID.getStringByID("config_auto", lang), "-1").withDefault(config.lang == null));

        for (CommonStatic.Lang.Locale loc : CommonStatic.Lang.supportedLanguage) {
            String l = LangID.getStringByID("lang_" + loc.code, config.lang);

            languages.add(SelectOption.of(LangID.getStringByID("config_locale", lang).replace("_", l), loc.code).withDefault(config.lang == loc));
        }

        Button extra;

        if(config.extra) {
            extra = Button.secondary("extra", LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_true", lang))).withEmoji(EmojiStore.SWITCHON);
        } else {
            extra = Button.secondary("extra", LangID.getStringByID("config_extra", lang).replace("_", LangID.getStringByID("data_false", lang))).withEmoji(EmojiStore.SWITCHOFF);
        }

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("button_confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("button_cancel", lang)));

        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT));

        replyToMessageSafely(ch, message, loader.getMessage(), a -> a.setComponents(
                ActionRow.of(Button.secondary("defLevels", String.format(LangID.getStringByID("config_setlevel", lang), config.defLevel)).withEmoji(Emoji.fromUnicode("⚙"))),
                ActionRow.of(extra),
                ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()),
                ActionRow.of(pages),
                ActionRow.of(components)
        ), msg -> {
            Message author = loader.getMessage();

            User u = author.getAuthor();

            StaticStore.putHolder(u.getId(), new ConfigButtonHolder(author, msg, config, holder, ch.getId(), config.lang == null ? holder.config.lang : config.lang));
        });
    }
}
