package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
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

public class ServerLocale extends ConstraintCommand {
    public ServerLocale(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String localeName;
        Emoji emoji;

        if (holder.config.lang == -1) {
            localeName = LangID.getStringByID("locale_server", lang);
            emoji = Emoji.fromUnicode("⚙️");
        } else {
            int index = -1;

            for (int i = 0; i < StaticStore.langIndex.length; i++) {
                if (holder.config.lang == StaticStore.langIndex[i]) {
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                StaticStore.logger.uploadLog("W/LocaleSettingHolder::onEvent - Unknown language ID : %d".formatted(holder.config.lang));

                index = LangID.EN;
            }

            localeName = LangID.getStringByID("lang_" + StaticStore.langCode[index], lang);
            emoji = Emoji.fromUnicode(StaticStore.langUnicode[index]);
        }

        replyToMessageSafely(ch, LangID.getStringByID("locale_select", holder.config.lang).formatted(emoji, localeName), loader.getMessage(), a -> a.setComponents(getComponents()), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new LocaleSettingHolder(loader.getMessage(), ch.getId(), msg, holder.config, holder, true))
        );
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();
        
        if (holder == null)
            return result;

        List<SelectOption> localeOptions = new ArrayList<>();

        for (int i = 0; i < StaticStore.langCode.length; i++) {
            String localeCode = StaticStore.langCode[i];
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[i]);
            
            localeOptions.add(SelectOption.of(LangID.getStringByID("lang_" + localeCode, holder.config.lang), localeCode).withEmoji(emoji).withDefault(holder.config.lang == StaticStore.langIndex[i]));
        }
        
        result.add(ActionRow.of(
                StringSelectMenu.create("locale")
                        .addOptions(localeOptions)
                        .setPlaceholder(LangID.getStringByID("locale_placeholder", holder.config.lang))
                        .setRequiredRange(1, 1)
                        .build()
        ));
        
        result.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("button_confirm", holder.config.lang))
        ));
        
        return result;
    }
}
