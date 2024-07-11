package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.LocaleSettingHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Locale extends ConstraintCommand {

    public Locale(ROLE role, CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(role, lang, holder, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ConfigHolder config = StaticStore.config.computeIfAbsent(loader.getUser().getId(), k -> new ConfigHolder());

        CommonStatic.Lang.Locale locale = config.lang;

        String localeName;
        Emoji emoji;

        if (locale == null) {
            localeName = LangID.getStringByID("locale_server", lang);
            emoji = Emoji.fromUnicode("⚙️");
        } else {


            localeName = LangID.getStringByID("lang_" + locale.code, lang);
            emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);
        }

        replyToMessageSafely(ch, LangID.getStringByID("locale_select", lang).formatted(emoji, localeName), loader.getMessage(), a -> a.setComponents(getComponents(config)), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new LocaleSettingHolder(loader.getMessage(), ch.getId(), msg, config, holder, false, lang))
        );
    }

    private List<LayoutComponent> getComponents(ConfigHolder config) {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> localeOptions = new ArrayList<>();

        localeOptions.add(SelectOption.of(LangID.getStringByID("locale_server", lang), "auto").withDescription(LangID.getStringByID("locale_serverdesc", lang)).withEmoji(Emoji.fromUnicode("⚙️")).withDefault(config.lang == null));

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            String localeCode = locale.code;
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            localeOptions.add(SelectOption.of(LangID.getStringByID("lang_" + localeCode, lang), localeCode).withEmoji(emoji).withDefault(config.lang == locale));
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("locale")
                        .addOptions(localeOptions)
                        .setPlaceholder(LangID.getStringByID("locale_placeholder", lang))
                        .setRequiredRange(1, 1)
                        .build()
        ));

        result.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("button_confirm", lang))
        ));

        return result;
    }
}
