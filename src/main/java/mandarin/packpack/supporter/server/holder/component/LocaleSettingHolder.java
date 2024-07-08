package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocaleSettingHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final boolean forServer;

    @Nullable
    private final IDHolder holder;

    public LocaleSettingHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull ConfigHolder config, @Nullable IDHolder holder, boolean forServer) {
        super(author, channelID, message);

        this.config = config;
        this.forServer = forServer;

        this.holder = holder;
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "locale" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                String localeCode = e.getValues().getFirst();

                if (localeCode.equals("auto")) {
                    config.lang = -1;
                } else {
                    int index = -1;

                    for (int i = 0; i < StaticStore.langCode.length; i++) {
                        if (localeCode.equals(StaticStore.langCode[i])) {
                            index = i;

                            break;
                        }
                    }

                    if (index == -1) {
                        StaticStore.logger.uploadLog("W/LocaleSettingHolder::onEvent - Unknown locale code : %s".formatted(localeCode));

                        return;
                    }

                    config.lang = StaticStore.langIndex[index];
                }

                int lang;

                if (config.lang == -1)
                    lang = holder != null ? holder.config.lang : config.lang;
                else
                    lang = config.lang;

                String localeName;
                Emoji emoji;

                if (config.lang == -1) {
                    localeName = LangID.getStringByID("locale_server", lang);
                    emoji = Emoji.fromUnicode("⚙️");
                } else {
                    int index = -1;

                    for (int i = 0; i < StaticStore.langIndex.length; i++) {
                        if (config.lang == StaticStore.langIndex[i]) {
                            index = i;
                            break;
                        }
                    }

                    if (index == -1) {
                        StaticStore.logger.uploadLog("W/LocaleSettingHolder::onEvent - Unknown language ID : %d".formatted(config.lang));

                        index = LangID.EN;
                    }

                    localeName = LangID.getStringByID("lang_" + StaticStore.langCode[index], lang);
                    emoji = Emoji.fromUnicode(StaticStore.langUnicode[index]);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("locale_select", lang).formatted(emoji, localeName))
                        .setComponents(getComponents())
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();
            }
            case "confirm" -> {
                int lang;

                if (config.lang == -1)
                    lang = holder != null ? holder.config.lang : config.lang;
                else
                    lang = config.lang;

                event.deferEdit()
                        .setContent(LangID.getStringByID("locale_confirm", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        int lang;

        if (config.lang == -1)
            lang = holder != null ? holder.config.lang : config.lang;
        else
            lang = config.lang;

        message.editMessage(LangID.getStringByID("locale_expire", lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<LayoutComponent> getComponents() {
        int lang;

        if (config.lang == -1)
            lang = holder != null ? holder.config.lang : config.lang;
        else
            lang = config.lang;

        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> localeOptions = new ArrayList<>();

        if (!forServer) {
            localeOptions.add(SelectOption.of(LangID.getStringByID("locale_server", lang), "auto").withDescription(LangID.getStringByID("locale_serverdesc", lang)).withEmoji(Emoji.fromUnicode("⚙️")).withDefault(config.lang == -1));
        }

        for (int i = 0; i < StaticStore.langCode.length; i++) {
            String localeCode = StaticStore.langCode[i];
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[i]);

            localeOptions.add(SelectOption.of(LangID.getStringByID("lang_" + localeCode, lang), localeCode).withEmoji(emoji).withDefault(config.lang != -1 && config.lang == StaticStore.langIndex[i]));
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
