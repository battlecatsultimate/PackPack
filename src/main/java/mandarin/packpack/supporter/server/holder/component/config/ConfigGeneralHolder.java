package mandarin.packpack.supporter.server.holder.component.config;

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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigGeneralHolder extends ServerConfigHolder {

    public ConfigGeneralHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                if (!(event instanceof StringSelectInteractionEvent se))
                    return;

                holder.config.lang = StaticStore.safeParseInt(se.getValues().getFirst());

                String languageName = switch (holder.config.lang) {
                    case LangID.EN -> LangID.getStringByID("lang_en", lang);
                    case LangID.ZH -> LangID.getStringByID("lang_zh", lang);
                    case LangID.KR -> LangID.getStringByID("lang_kr", lang);
                    case LangID.JP -> LangID.getStringByID("lang_jp", lang);
                    case LangID.FR -> LangID.getStringByID("lang_fr", lang);
                    case LangID.IT -> LangID.getStringByID("lang_it", lang);
                    case LangID.ES -> LangID.getStringByID("lang_es", lang);
                    case LangID.DE -> LangID.getStringByID("lang_de", lang);
                    case LangID.TH -> LangID.getStringByID("lang_th", lang);
                    default -> throw new IllegalStateException("E/ConfigGeneralHolder::getContents - Unrecognized locale " + holder.config.lang + " detected");
                };

                event.deferReply()
                        .setContent(LangID.getStringByID("sercon_langset", lang).formatted(languageName))
                        .setAllowedMentions(new ArrayList<>())
                        .setEphemeral(true)
                        .queue();

                applyResult();
            }
            case "prefix" -> {
                TextInput input = TextInput.create("prefix", LangID.getStringByID("sercon_prefix", lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("sercon_prefixdesc", lang))
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create("prefix", LangID.getStringByID("sercon_prefixmodal", lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                connectTo(new ServerPrefixModalHolder(getAuthorMessage(), channelID, message, holder.config, lang));
            }
            case "role" -> connectTo(event, new ConfigRoleRegistrationHolder(getAuthorMessage(), channelID, message, holder, backup, lang));
            case "confirm" -> {
                event.deferEdit()
                        .setContent(LangID.getStringByID("sercon_done", lang))
                        .setComponents()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;
            }
            case "cancel" -> {
                registerPopUp(event, LangID.getStringByID("sercon_cancelask", lang), lang);

                connectTo(new ConfirmPopUpHolder(getAuthorMessage(), channelID, message, e -> {
                    e.deferEdit()
                            .setContent(LangID.getStringByID("sercon_cancel", lang))
                            .setComponents()
                            .setAllowedMentions(new ArrayList<>())
                            .mentionRepliedUser(false)
                            .queue();

                    holder.inject(backup);

                    expired = true;
                }, lang));
            }
            case "back" -> goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    @Override
    public void onBack(@NotNull GenericComponentInteractionCreateEvent event, Holder child) {
        applyResult(event);
    }

    @Override
    public void onBack(Holder child) {
        applyResult();
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
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
        Emoji e = switch (holder.config.lang) {
            case LangID.EN -> Emoji.fromUnicode("🇺🇸");
            case LangID.ZH -> Emoji.fromUnicode("🇹🇼");
            case LangID.KR -> Emoji.fromUnicode("🇰🇷");
            case LangID.JP -> Emoji.fromUnicode("🇯🇵");
            case LangID.FR -> Emoji.fromUnicode("🇫🇷");
            case LangID.IT -> Emoji.fromUnicode("🇮🇹");
            case LangID.ES -> Emoji.fromUnicode("🇪🇸");
            case LangID.DE -> Emoji.fromUnicode("🇩🇪");
            case LangID.TH -> Emoji.fromUnicode("🇹🇭");
            default -> throw new IllegalStateException("E/ConfigGeneralHolder::getContents - Unrecognized locale " + holder.config.lang + " detected");
        };

        String languageName = switch (holder.config.lang) {
            case LangID.EN -> LangID.getStringByID("lang_en", lang);
            case LangID.ZH -> LangID.getStringByID("lang_zh", lang);
            case LangID.KR -> LangID.getStringByID("lang_kr", lang);
            case LangID.JP -> LangID.getStringByID("lang_jp", lang);
            case LangID.FR -> LangID.getStringByID("lang_fr", lang);
            case LangID.IT -> LangID.getStringByID("lang_it", lang);
            case LangID.ES -> LangID.getStringByID("lang_es", lang);
            case LangID.DE -> LangID.getStringByID("lang_de", lang);
            case LangID.TH -> LangID.getStringByID("lang_th", lang);
            default -> throw new IllegalStateException("E/ConfigGeneralHolder::getContents - Unrecognized locale " + holder.config.lang + " detected");
        };

        return LangID.getStringByID("sercon_gentit", lang) + "\n" +
                LangID.getStringByID("sercon_genlang", lang).formatted(EmojiStore.LANGUAGE.getFormatted(), e.getFormatted(), languageName) + "\n" +
                LangID.getStringByID("sercon_genlangdesc", lang).formatted(languageName) + "\n" +
                LangID.getStringByID("sercon_genpre", lang).formatted(Emoji.fromUnicode("🔗").getFormatted(), holder.config.prefix) + "\n" +
                LangID.getStringByID("sercon_genpredesc", lang).formatted(StaticStore.globalPrefix, StaticStore.globalPrefix);
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        List<SelectOption> languageOptions = new ArrayList<>();

        for (int j = 0; j < StaticStore.langIndex.length; j++) {
            String l = LangID.getStringByID("lang_" + StaticStore.langCode[j], lang);

            Emoji e = switch (StaticStore.langIndex[j]) {
                case LangID.EN -> Emoji.fromUnicode("🇺🇸");
                case LangID.ZH -> Emoji.fromUnicode("🇹🇼");
                case LangID.KR -> Emoji.fromUnicode("🇰🇷");
                case LangID.JP -> Emoji.fromUnicode("🇯🇵");
                case LangID.FR -> Emoji.fromUnicode("🇫🇷");
                case LangID.IT -> Emoji.fromUnicode("🇮🇹");
                case LangID.ES -> Emoji.fromUnicode("🇪🇸");
                case LangID.DE -> Emoji.fromUnicode("🇩🇪");
                case LangID.TH -> Emoji.fromUnicode("🇹🇭");
                case LangID.RU -> Emoji.fromUnicode("🇷🇺");
                default -> throw new IllegalStateException("E/ConfigGeneralHolder::getComponents - Unrecognized locale " + StaticStore.langCode[j] + " detected");
            };

            if (holder.config.lang == StaticStore.langIndex[j]) {
                languageOptions.add(
                        SelectOption.of(
                                LangID.getStringByID("config_locale", lang).replace("_", l),
                                String.valueOf(StaticStore.langIndex[j])
                        )
                                .withDefault(true)
                                .withEmoji(e)
                );
            } else {
                languageOptions.add(
                        SelectOption.of(
                                LangID.getStringByID("config_locale", lang).replace("_", l),
                                String.valueOf(StaticStore.langIndex[j])
                        )
                                .withEmoji(e)
                );
            }
        }

        result.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languageOptions).setPlaceholder(LangID.getStringByID("sercon_lang", lang)).build()));
        result.add(ActionRow.of(Button.secondary("prefix", LangID.getStringByID("sercon_prefixbutton", lang)).withEmoji(Emoji.fromUnicode("🔗"))));
        result.add(ActionRow.of(Button.secondary("role", LangID.getStringByID("sercon_role", lang)).withEmoji(EmojiStore.ROLE)));
        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}
