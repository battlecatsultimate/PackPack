package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LocaleSettingHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final boolean forServer;

    @Nullable
    private final IDHolder holder;

    public LocaleSettingHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull ConfigHolder config, @Nullable IDHolder holder, boolean forServer, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.forServer = forServer;

        this.holder = holder;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "locale" -> {
                if (!(event instanceof StringSelectInteractionEvent e))
                    return;

                String localeCode = e.getValues().getFirst();

                if (localeCode.equals("auto")) {
                    config.lang = null;
                } else {
                    config.lang = CommonStatic.Lang.Locale.valueOf(localeCode);
                }

                CommonStatic.Lang.Locale lang;

                if (config.lang == null)
                    lang = holder != null ? holder.config.lang : CommonStatic.Lang.Locale.EN;
                else
                    lang = config.lang;

                String localeName;
                Emoji emoji;

                if (config.lang == null) {
                    localeName = LangID.getStringByID("bot.language.auto", lang);
                    emoji = Emoji.fromUnicode("⚙️");
                } else {
                    localeName = LangID.getStringByID("bot.language." + config.lang.code, lang);
                    emoji = Emoji.fromUnicode(StaticStore.langUnicode[config.lang.ordinal()]);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("locale.select", lang).formatted(emoji, localeName))
                        .setComponents(getComponents())
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();
            }
            case "confirm" -> {
                CommonStatic.Lang.Locale lang;

                if (config.lang == null)
                    lang = holder != null ? holder.config.lang : CommonStatic.Lang.Locale.EN;
                else
                    lang = config.lang;

                event.deferEdit()
                        .setContent(LangID.getStringByID("locale.confirmed", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        CommonStatic.Lang.Locale lang;

        if (config.lang == null)
            lang = holder != null ? holder.config.lang : CommonStatic.Lang.Locale.EN;
        else
            lang = config.lang;

        message.editMessage(LangID.getStringByID("locale.expired", lang))
                .setComponents()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private List<MessageTopLevelComponent> getComponents() {
        CommonStatic.Lang.Locale lang;

        if (config.lang == null)
            lang = holder != null ? holder.config.lang : CommonStatic.Lang.Locale.EN;
        else
            lang = config.lang;

        List<MessageTopLevelComponent> result = new ArrayList<>();

        List<SelectOption> localeOptions = new ArrayList<>();

        if (!forServer) {
            localeOptions.add(SelectOption.of(LangID.getStringByID("bot.language.auto", lang), "auto").withDescription(LangID.getStringByID("locale.followingServer", lang)).withEmoji(Emoji.fromUnicode("⚙️")).withDefault(config.lang == null));
        }

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            localeOptions.add(SelectOption.of(LangID.getStringByID("bot.language." + locale.code, lang), locale.name()).withEmoji(emoji).withDefault(config.lang == locale));
        }

        result.add(ActionRow.of(
                StringSelectMenu.create("locale")
                        .addOptions(localeOptions)
                        .setPlaceholder(LangID.getStringByID("locale.selectList", lang))
                        .setRequiredRange(1, 1)
                        .build()
        ));

        result.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("ui.button.confirm", lang))
        ));

        return result;
    }
}
