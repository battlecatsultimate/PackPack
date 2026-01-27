package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.config.user.ConfigButtonHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;

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

        replyToMessageSafely(ch, loader.getMessage(), msg -> {
            CommonStatic.Lang.Locale locale = config.lang == null ? holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang : config.lang;

            StaticStore.putHolder(loader.getUser().getId(), new ConfigButtonHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, config, holder, locale));
        }, getComponents());
    }

    private Container getComponents() {
        List<ContainerChildComponent> components = new ArrayList<>();

        components.add(Section.of(
                Button.secondary("defLevels", LangID.getStringByID("config.defaultLevel.set.title", lang)).withEmoji(Emoji.fromUnicode("⚙️")),
                TextDisplay.of(
                        "### " + LangID.getStringByID("config.defaultLevel.title", lang) + "\n" +
                               "**" + LangID.getStringByID("config.defaultLevel.value", lang).formatted(config.defLevel) + "**\n\n" +
                               LangID.getStringByID("config.defaultLevel.description", lang).formatted(config.defLevel)
                )
        ));

        components.add(Separator.create(true, Separator.Spacing.LARGE));

        String language;

        if (config.lang == null) {
            language = LangID.getStringByID("config.locale.auto", lang);
        } else {
            language = switch(config.lang) {
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

        String currentLocale = LangID.getStringByID("config.locale.value", lang).formatted(language);

        components.add(TextDisplay.of(
                "### " + LangID.getStringByID("config.locale.title", lang) + "\n" +
                       "**" + currentLocale + "**"
        ));

        List<SelectOption> localeOptions = new ArrayList<>();

        localeOptions.add(SelectOption.of(LangID.getStringByID("config.locale.auto", lang), "auto").withDefault(config.lang == null));

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            localeOptions.add(SelectOption.of(LangID.getStringByID("bot.language." + locale.code, holder.config.lang), locale.name()).withEmoji(emoji).withDefault(config.lang == locale));
        }

        components.add(ActionRow.of(StringSelectMenu.create("locale").addOptions(localeOptions).setPlaceholder(LangID.getStringByID("locale.selectList", lang)).setRequiredRange(1, 1).build()));

        components.add(Separator.create(true, Separator.Spacing.LARGE));

        Emoji compactEmbedSwitch;
        String compactEmbedValue;
        String compactEmbedDescription;

        if (config.compact) {
            compactEmbedSwitch = EmojiStore.SWITCHON;
            compactEmbedValue = LangID.getStringByID("data.true", lang);
            compactEmbedDescription = LangID.getStringByID("config.compactEmbed.description.true", lang);
        } else {
            compactEmbedSwitch = EmojiStore.SWITCHOFF;
            compactEmbedValue = LangID.getStringByID("data.false", lang);
            compactEmbedDescription = LangID.getStringByID("config.compactEmbed.description.false", lang);
        }

        components.add(Section.of(
                Button.secondary("compact", compactEmbedValue).withEmoji(compactEmbedSwitch),
                TextDisplay.of(
                        "### " + LangID.getStringByID("config.compactEmbed.title", lang) + "\n" +
                               "**" + LangID.getStringByID("config.compactEmbed.value", lang).formatted(compactEmbedValue) + "**\n\n" +
                               compactEmbedDescription
                )
        ));

        components.add(Separator.create(true, Separator.Spacing.LARGE));

        components.add(ActionRow.of(
                Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled(),
                Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT)
        ));

        components.add(Separator.create(false, Separator.Spacing.SMALL));

        components.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return Container.of(components);
    }
}
