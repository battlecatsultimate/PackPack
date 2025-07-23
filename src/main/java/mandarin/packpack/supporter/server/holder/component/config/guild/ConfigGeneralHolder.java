package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.ServerPrefixModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigGeneralHolder extends ServerConfigHolder {

    public ConfigGeneralHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                if (!(event instanceof StringSelectInteractionEvent se))
                    return;

                holder.config.lang = CommonStatic.Lang.Locale.valueOf(se.getValues().getFirst());

                String languageName = switch (holder.config.lang) {
                    case EN -> LangID.getStringByID("bot.language.en", lang);
                    case ZH -> LangID.getStringByID("bot.language.zh", lang);
                    case KR -> LangID.getStringByID("bot.language.kr", lang);
                    case JP -> LangID.getStringByID("bot.language.jp", lang);
                    case FR -> LangID.getStringByID("bot.language.fr", lang);
                    case IT -> LangID.getStringByID("bot.language.it", lang);
                    case ES -> LangID.getStringByID("bot.language.es", lang);
                    case DE -> LangID.getStringByID("bot.language.de", lang);
                    case TH -> LangID.getStringByID("bot.language.th", lang);
                    case RU -> LangID.getStringByID("bot.language.ru", lang);
                };

                event.deferReply()
                        .setContent(LangID.getStringByID("serverConfig.general.languageSet", lang).formatted(languageName))
                        .setAllowedMentions(new ArrayList<>())
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "prefix" -> {
                TextInput input = TextInput.create("prefix", LangID.getStringByID("serverConfig.general.prefix", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("serverConfig.general.typePrefix", lang))
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("prefix", LangID.getStringByID("serverConfig.general.serverPrefix", lang))
                        .addComponents(ActionRow.of(input))
                        .build();

                event.replyModal(modal).queue();

                connectTo(new ServerPrefixModalHolder(getAuthorMessage(), userID, channelID, message, holder, lang));
            }
            case "role" -> connectTo(event, new ConfigRoleRegistrationHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("serverConfig.applied", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("serverConfig.cancelConfirm", lang));

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), userID, channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("serverConfig.canceled", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    end(true);
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        applyResult(event);
    }

    @Override
    public void onBack(@Nonnull Holder child) {
        applyResult();
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult() {
        message.editMessage(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        CommonStatic.Lang.Locale locale = Objects.requireNonNull(holder.config.lang);

        Emoji e = switch (locale) {
            case EN -> Emoji.fromUnicode("🇺🇸");
            case ZH -> Emoji.fromUnicode("🇹🇼");
            case KR -> Emoji.fromUnicode("🇰🇷");
            case JP -> Emoji.fromUnicode("🇯🇵");
            case FR -> Emoji.fromUnicode("🇫🇷");
            case IT -> Emoji.fromUnicode("🇮🇹");
            case ES -> Emoji.fromUnicode("🇪🇸");
            case DE -> Emoji.fromUnicode("🇩🇪");
            case TH -> Emoji.fromUnicode("🇹🇭");
            case RU -> Emoji.fromUnicode("🇷🇺");
        };

        String languageName = switch (locale) {
            case EN -> LangID.getStringByID("bot.language.en", lang);
            case ZH -> LangID.getStringByID("bot.language.zh", lang);
            case KR -> LangID.getStringByID("bot.language.kr", lang);
            case JP -> LangID.getStringByID("bot.language.jp", lang);
            case FR -> LangID.getStringByID("bot.language.fr", lang);
            case IT -> LangID.getStringByID("bot.language.it", lang);
            case ES -> LangID.getStringByID("bot.language.es", lang);
            case DE -> LangID.getStringByID("bot.language.de", lang);
            case TH -> LangID.getStringByID("bot.language.th", lang);
            case RU -> LangID.getStringByID("bot.language.ru", lang);
        };

        return LangID.getStringByID("serverConfig.general.documentation.title", lang) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.language.title", lang).formatted(EmojiStore.LANGUAGE.getFormatted(), e.getFormatted(), languageName) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.language.description", lang).formatted(languageName) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.prefix.title", lang).formatted(Emoji.fromUnicode("🔗").getFormatted(), holder.config.prefix) + "\n" +
                LangID.getStringByID("serverConfig.general.documentation.prefix.description", lang).formatted(StaticStore.globalPrefix, StaticStore.globalPrefix);
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        List<SelectOption> languageOptions = new ArrayList<>();

        for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.Locale.values()) {
            String l = LangID.getStringByID("bot.language." + locale.code, lang);
            Emoji e = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

            languageOptions.add(
                    SelectOption.of(
                                    LangID.getStringByID("config.locale.title", lang).replace("_", l),
                                    locale.name()
                            )
                            .withDefault(holder.config.lang == locale)
                            .withEmoji(e)
            );
        }

        result.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languageOptions).setPlaceholder(LangID.getStringByID("serverConfig.general.selectLanguage", lang)).build()));
        result.add(ActionRow.of(Button.secondary("prefix", LangID.getStringByID("serverConfig.general.prefixSet", lang)).withEmoji(Emoji.fromUnicode("🔗"))));
        result.add(ActionRow.of(Button.secondary("role", LangID.getStringByID("serverConfig.general.role.button", lang)).withEmoji(EmojiStore.ROLE)));
        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
