package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.ConfigButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import javax.annotation.Nonnull;
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
    public void doSomething(@Nonnull CommandLoader loader) {
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
                case RU -> LangID.getStringByID("bot.language.ru", lang);
            };
        }

        String compactTitle = LangID.getStringByID(config.compact ? "data.true" : "data.false", lang);
        String compactDescription = LangID.getStringByID(config.compact ? "config.compactEmbed.description.true" : "config.compactEmbed.description.false", lang);

        String message = "**" + LangID.getStringByID("config.defaultLevel.title", lang).replace("_", String.valueOf(config.defLevel)) + "**\n\n" +
                LangID.getStringByID("config.defaultLevel.description", lang).replace("_", String.valueOf(config.defLevel)) + "\n\n" +
                "**" + LangID.getStringByID("config.locale.title", lang).replace("_", locale) + "**" + "\n\n" +
                "**" + LangID.getStringByID("config.compactEmbed.title", lang).replace("_", compactTitle) + "**" + "\n\n" +
                compactDescription;

        List<SelectOption> languages = new ArrayList<>();

        languages.add(SelectOption.of(LangID.getStringByID("config.locale.auto", lang), "auto").withDefault(config.lang == null));

        for (CommonStatic.Lang.Locale loc : StaticStore.supportedLanguages) {
            String l = LangID.getStringByID("bot.language." + loc.code, config.lang);

            languages.add(SelectOption.of(LangID.getStringByID("config.locale.title", lang).replace("_", l), loc.name()).withDefault(config.lang == loc));
        }

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        List<ActionComponent> pages = new ArrayList<>();

        pages.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
        pages.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT));

        Button compact;

        if(config.compact) {
            compact = Button.secondary("compact", LangID.getStringByID("config.compactEmbed.title", lang).replace("_", LangID.getStringByID("data.true", lang))).withEmoji(EmojiStore.SWITCHON);
        } else {
            compact = Button.secondary("compact", LangID.getStringByID("config.compactEmbed.title", lang).replace("_", LangID.getStringByID("data.false", lang))).withEmoji(EmojiStore.SWITCHOFF);
        }

        replyToMessageSafely(ch, message, loader.getMessage(), a -> a.setComponents(
                ActionRow.of(Button.secondary("defLevels", String.format(LangID.getStringByID("config.defaultLevel.set.title", lang), config.defLevel)).withEmoji(Emoji.fromUnicode("⚙"))),
                ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()),
                ActionRow.of(compact),
                ActionRow.of(pages),
                ActionRow.of(components)
        ), msg -> {
            Message author = loader.getMessage();

            User u = author.getAuthor();

            CommonStatic.Lang.Locale l;

            if (config.lang == null) {
                l = holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang;
            } else {
                l = config.lang;
            }


            StaticStore.putHolder(u.getId(), new ConfigButtonHolder(author, u.getId(), ch.getId(), msg, config, holder, l));
        });
    }
}
