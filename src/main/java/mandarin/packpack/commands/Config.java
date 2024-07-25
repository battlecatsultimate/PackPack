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
            locale = LangID.getStringByID("config.locale.auto", lang);
        } else {
            locale = switch (language) {
                case EN -> LangID.getStringByID("bot.language.en", lang);
                case JP -> LangID.getStringByID("bot.language.jp", lang);
                case KR -> LangID.getStringByID("bot.language.kr", lang);
                case ZH -> LangID.getStringByID("bot.language.zh", lang);
                case FR -> LangID.getStringByID("bot.language.fr", lang);
                case IT -> LangID.getStringByID("bot.language.it", lang);
                case ES -> LangID.getStringByID("bot.language.es", lang);
                case DE -> LangID.getStringByID("bot.language.de", lang);
                case TH -> LangID.getStringByID("bot.language.th", lang);
                default -> LangID.getStringByID("config.locale.auto", lang);
            };
        }

        String ex = LangID.getStringByID(config.extra ? "config.extraInformation.description.true" : "config.extraInformation.description.false", lang);
        String bool = LangID.getStringByID(config.extra ? "data.true" : "data.false", lang);

        String message = "**" + LangID.getStringByID("config.defaultLevel.title", lang).replace("_", String.valueOf(config.defLevel)) + "**\n\n" +
                LangID.getStringByID("config.defaultLevel.description", lang).replace("_", String.valueOf(config.defLevel)) + "\n\n" +
                "**" + LangID.getStringByID("config.extraInformation.title", lang).replace("_", bool) + "**\n\n" +
                ex + "\n\n" +
                "**" + LangID.getStringByID("config.locale.title", lang).replace("_", locale) + "**";

        List<SelectOption> languages = new ArrayList<>();

        languages.add(SelectOption.of(LangID.getStringByID("config.locale.auto", lang), "auto").withDefault(config.lang == null));

        for (CommonStatic.Lang.Locale loc : CommonStatic.Lang.supportedLanguage) {
            String l = LangID.getStringByID("bot.language." + loc.code, config.lang);

            languages.add(SelectOption.of(LangID.getStringByID("config.locale.title", lang).replace("_", l), loc.name()).withDefault(config.lang == loc));
        }

        Button extra;

        if(config.extra) {
            extra = Button.secondary("extra", LangID.getStringByID("config.extraInformation.title", lang).replace("_", LangID.getStringByID("data.true", lang))).withEmoji(EmojiStore.SWITCHON);
        } else {
            extra = Button.secondary("extra", LangID.getStringByID("config.extraInformation.title", lang).replace("_", LangID.getStringByID("data.false", lang))).withEmoji(EmojiStore.SWITCHOFF);
        }

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT));

        replyToMessageSafely(ch, message, loader.getMessage(), a -> a.setComponents(
                ActionRow.of(Button.secondary("defLevels", String.format(LangID.getStringByID("config.defaultLevel.set.title", lang), config.defLevel)).withEmoji(Emoji.fromUnicode("⚙"))),
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
