package mandarin.packpack.supporter.server.holder.component.config.user;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.modal.LevelModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigButtonHolder extends ComponentHolder {
    private static final int TOTAL_CONFIG = 7;
    
    private final ConfigHolder config;
    private final ConfigHolder backup;
    private final IDHolder holder;

    private int page = 0;

    public ConfigButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);
        
        this.config = config;
        this.backup = config.clone();
        this.holder = holder;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "language" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;

                String code = es.getValues().getFirst();

                if (code.equals("auto")) {
                    config.lang = null;
                } else {
                    config.lang = CommonStatic.Lang.Locale.valueOf(code);
                }
                
                performResult(event);
            }
            case "defLevels" -> {
                TextInput input = TextInput.create("level", LangID.getStringByID("config.defaultLevel.set.inputTagName", config.lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("config.defaultLevel.set.placeholder", config.lang))
                        .setRequiredRange(1, 2)
                        .setRequired(true)
                        .setValue(String.valueOf(config.defLevel))
                        .build();

                Modal modal = Modal.create("level", LangID.getStringByID("config.defaultLevel.set.tagName", config.lang))
                        .addActionRow(input)
                        .build();
                
                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new LevelModalHolder(getAuthorMessage(), userID, channelID, message, config, e -> e.deferEdit()
                        .setContent(parseMessage())
                        .setComponents(parseComponents())
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue(), lang));
            }
            case "unit" -> {
                StringSelectInteractionEvent es = (StringSelectInteractionEvent) event;
                
                if (es.getValues().size() != 1)
                    return;
                
                config.useFrame = es.getValues().getFirst().equals("frame");
                
                performResult(event);
            }
            case "compact" -> {
                config.compact = !config.compact;
                
                performResult(event);
            }
            case "trueForm" -> {
                config.trueForm = !config.trueForm;
                
                performResult(event);
            }
            case "treasure" -> {
                config.treasure = !config.treasure;

                performResult(event);
            }
            case "embed" -> connectTo(event, new CommandListHolder(getAuthorMessage(), userID, channelID, message, config, backup, config.lang == null ? holder.config.lang : config.lang));
            case "next" -> {
                page++;
                performResult(event);
            }
            case "prev" -> {
                page--;
                performResult(event);
            }
            case "confirm" -> {
                CommonStatic.Lang.Locale lang = config.lang;

                if (lang == null)
                    lang = holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang;

                event.deferEdit()
                        .setContent(LangID.getStringByID("config.applied", lang))
                        .setComponents()
                        .queue();

                if (!StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, config);
                }

                end(true);
            }
            case "cancel" -> {
                if(StaticStore.config.containsKey(userID)) {
                    StaticStore.config.put(userID, backup);
                }

                event.deferEdit()
                        .setContent(LangID.getStringByID("config.canceled", backup.lang))
                        .setComponents()
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
        if(StaticStore.config.containsKey(userID)) {
            StaticStore.config.put(userID, backup);
        }

        message.editMessage(LangID.getStringByID("config.expired", config.lang == null ? holder.config.lang : config.lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        performResult(event);
    }

    private void performResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(parseMessage())
                .setComponents(parseComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    private List<ActionRow> parseComponents() {
        CommonStatic.Lang.Locale lang = config.lang;

        if(lang == null)
            lang = holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang;

        List<ActionRow> m = new ArrayList<>();

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0 -> m.add(ActionRow.of(Button.secondary("defLevels", String.format(LangID.getStringByID("config.defaultLevel.set.title", lang), config.defLevel)).withEmoji(Emoji.fromUnicode("⚙"))));
                case 1 -> {
                    List<SelectOption> languages = new ArrayList<>();

                    languages.add(SelectOption.of(LangID.getStringByID("config.locale.auto", lang), "auto").withDefault(config.lang == null));

                    for (CommonStatic.Lang.Locale locale : StaticStore.supportedLanguages) {
                        String l = LangID.getStringByID("bot.language." + locale.code, lang);

                        languages.add(
                                SelectOption.of(LangID.getStringByID("config.locale.title", lang).replace("_", l), locale.name())
                                        .withDefault(config.lang == locale)
                        );
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("language").addOptions(languages).build()));
                }
                case 2 -> {
                    if(config.compact) {
                        m.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("config.compactEmbed.title", lang).replace("_", LangID.getStringByID("data.true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("config.compactEmbed.title", lang).replace("_", LangID.getStringByID("data.false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 3 -> {
                    if(config.trueForm) {
                        m.add(ActionRow.of(Button.secondary("trueForm", String.format(LangID.getStringByID("config.trueForm.title", lang), LangID.getStringByID("data.true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("trueForm", String.format(LangID.getStringByID("config.trueForm.title", lang), LangID.getStringByID("data.false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 4 -> {
                    List<SelectOption> units = new ArrayList<>();

                    if (config.useFrame) {
                        units.add(SelectOption.of(LangID.getStringByID("config.defaultUnit.unit", lang).replace("_", LangID.getStringByID("config.defaultUnit.frame", lang)), "frame").withDefault(true));
                        units.add(SelectOption.of(LangID.getStringByID("config.defaultUnit.unit", lang).replace("_", LangID.getStringByID("config.defaultUnit.second", lang)), "second"));
                    } else {
                        units.add(SelectOption.of(LangID.getStringByID("config.defaultUnit.unit", lang).replace("_", LangID.getStringByID("config.defaultUnit.frame", lang)), "frame"));
                        units.add(SelectOption.of(LangID.getStringByID("config.defaultUnit.unit", lang).replace("_", LangID.getStringByID("config.defaultUnit.second", lang)), "second").withDefault(true));
                    }

                    m.add(ActionRow.of(StringSelectMenu.create("unit").addOptions(units).build()));
                }
                case 5 -> {
                    if(config.treasure) {
                        m.add(ActionRow.of(Button.secondary("treasure", String.format(LangID.getStringByID("config.treasure.title", lang), LangID.getStringByID("data.true", lang))).withEmoji(EmojiStore.SWITCHON)));
                    } else {
                        m.add(ActionRow.of(Button.secondary("treasure", String.format(LangID.getStringByID("config.treasure.title", lang), LangID.getStringByID("data.false", lang))).withEmoji(EmojiStore.SWITCHOFF)));
                    }
                }
                case 6 -> m.add(ActionRow.of(Button.secondary("embed",LangID.getStringByID("config.commandList.button", lang)).withEmoji(Emoji.fromUnicode("🎛️"))));
            }
        }

        List<ActionComponent> pages = new ArrayList<>();

        if(page == 0) {
            pages.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).asDisabled());
            pages.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT));
        } else if((page + 1) * 3 >= TOTAL_CONFIG) {
            pages.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT).asDisabled());
        } else {
            pages.add(Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS));
            pages.add(Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT));
        }

        m.add(ActionRow.of(pages));

        List<ActionComponent> components = new ArrayList<>();

        components.add(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)));
        components.add(Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)));

        m.add(ActionRow.of(components));

        return m;
    }

    private String parseMessage() {
        CommonStatic.Lang.Locale lang = config.lang;

        if(lang == null)
            lang = holder == null ? CommonStatic.Lang.Locale.EN : holder.config.lang;

        StringBuilder message = new StringBuilder();

        for(int i = page * 3; i < (page + 1) * 3; i++) {
            switch (i) {
                case 0 -> message.append("**")
                        .append(LangID.getStringByID("config.defaultLevel.title", lang).replace("_", String.valueOf(config.defLevel)))
                        .append("**\n\n")
                        .append(LangID.getStringByID("config.defaultLevel.description", lang).replace("_", String.valueOf(config.defLevel)));
                case 1 -> {
                    String locale;

                    if (config.lang == null) {
                        locale = LangID.getStringByID("config.locale.auto", lang);
                    } else {
                        locale = switch (config.lang) {
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

                    message.append("**")
                            .append(LangID.getStringByID("config.locale.title", lang).replace("_", locale))
                            .append("**");
                }
                case 2 -> {
                    String compact = LangID.getStringByID(config.compact ? "data.true" : "data.false", lang);
                    String comp = LangID.getStringByID(config.compact ? "config.compactEmbed.description.true" : "config.compactEmbed.description.false", lang);

                    message.append("**")
                            .append(LangID.getStringByID("config.compactEmbed.title", lang).replace("_", compact))
                            .append("**\n\n")
                            .append(comp);
                }
                case 3 -> {
                    String trueForm = LangID.getStringByID(config.trueForm ? "data.true" : "data.false", lang);
                    String tr = LangID.getStringByID(config.trueForm ? "config.trueForm.description.true" : "config.trueForm.description.false", lang);

                    message.append("**")
                            .append(String.format(LangID.getStringByID("config.trueForm.title", lang), trueForm))
                            .append("**\n\n")
                            .append(tr);
                }
                case 4 -> {
                    String unit = LangID.getStringByID(config.useFrame ? "config.defaultUnit.frame" : "config.defaultUnit.second", lang);

                    message.append("**")
                            .append(LangID.getStringByID("config.defaultUnit.title", lang).replace("_", unit))
                            .append("**\n\n")
                            .append(LangID.getStringByID("config.defaultUnit.description", lang));
                }
                case 5 -> {
                    String treasure = LangID.getStringByID(config.treasure ? "data.true" : "data.false", lang);
                    String treasureText = LangID.getStringByID(config.treasure ? "config.treasure.description.true" : "config.treasure.description.false", lang);

                    message.append("**")
                            .append(String.format(LangID.getStringByID("config.treasure.title", lang), treasure))
                            .append("**\n\n")
                            .append(treasureText);
                }
            }

            if(i < (page + 1) * 3 - 1) {
                message.append("\n\n");
            }
        }

        return message.toString();
    }
}
