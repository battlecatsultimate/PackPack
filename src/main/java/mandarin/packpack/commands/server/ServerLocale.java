package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.LocaleSettingHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class ServerLocale extends ConstraintCommand {
    public ServerLocale(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String localeName;
        Emoji emoji;

        if (holder.config.lang == null) {
            localeName = LangID.getStringByID("bot.language.auto", lang);
            emoji = Emoji.fromUnicode("⚙️");
        } else {
            localeName = LangID.getStringByID("bot.language." + holder.config.lang.code, lang);
            emoji = Emoji.fromUnicode(StaticStore.langUnicode[lang.ordinal()]);
        }

        replyToMessageSafely(ch, LangID.getStringByID("locale.select", holder.config.lang).formatted(emoji, localeName), loader.getMessage(), a -> a.setComponents(getComponents()), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new LocaleSettingHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, holder.config, holder, true, lang))
        );
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();
        
        if (holder == null)
            return result;

        List<SelectOption> localeOptions = new ArrayList<>();

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            localeOptions.add(SelectOption.of(LangID.getStringByID("bot.language." + locale.code, holder.config.lang), locale.name()).withEmoji(emoji).withDefault(holder.config.lang == locale));
        }
        
        result.add(ActionRow.of(
                StringSelectMenu.create("locale")
                        .addOptions(localeOptions)
                        .setPlaceholder(LangID.getStringByID("locale.selectList", holder.config.lang))
                        .setRequiredRange(1, 1)
                        .build()
        ));
        
        result.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("ui.button.confirm", holder.config.lang))
        ));
        
        return result;
    }
}
