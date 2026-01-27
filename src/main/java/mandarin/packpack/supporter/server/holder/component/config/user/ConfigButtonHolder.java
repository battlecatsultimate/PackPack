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
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigButtonHolder extends ComponentHolder {
    private static final int TOTAL_CONFIG = 7;
    private static final int CONFIG_CHUNK = 3;
    
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
            case "locale" -> {
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
                TextInput input = TextInput.create("level", TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("config.defaultLevel.set.placeholder", config.lang))
                        .setRequiredRange(1, 2)
                        .setRequired(true)
                        .setValue(String.valueOf(config.defLevel))
                        .build();

                Modal modal = Modal.create("level", LangID.getStringByID("config.defaultLevel.set.tagName", config.lang))
                        .addComponents(Label.of(LangID.getStringByID("config.defaultLevel.set.inputTagName", config.lang), input))
                        .build();
                
                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new LevelModalHolder(getAuthorMessage(), userID, channelID, message, config, e -> e.deferEdit()
                        .setComponents(getComponents())
                        .useComponentsV2()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(new ArrayList<>())
                        .queue(), lang));
            }
            case "unit" -> {
                config.useFrame = !config.useFrame;
                
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
                        .setComponents(TextDisplay.of(LangID.getStringByID("config.applied", lang)))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
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
                        .setComponents(TextDisplay.of(LangID.getStringByID("config.canceled", backup.lang)))
                        .useComponentsV2()
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
        if(StaticStore.config.containsKey(userID)) {
            StaticStore.config.put(userID, backup);
        }

        message.editMessageComponents(TextDisplay.of(LangID.getStringByID("config.expired", config.lang == null ? holder.config.lang : config.lang)))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        performResult(event);
    }

    private void performResult(IMessageEditCallback event) {
        event.deferEdit()
                .setComponents(getComponents())
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private Container getComponents() {
        CommonStatic.Lang.Locale l;

        if (config.lang == null) {
            if (holder == null || holder.config.lang == null)
                l = CommonStatic.Lang.Locale.EN;
            else
                l = holder.config.lang;
        } else {
            l = config.lang;
        }

        List<ContainerChildComponent> components = new ArrayList<>();

        for(int i = page * CONFIG_CHUNK; i < Math.min(TOTAL_CONFIG, (page + 1) * CONFIG_CHUNK); i++) {
            switch (i) {
                case 0 ->
                    components.add(Section.of(
                            Button.secondary("defLevels", LangID.getStringByID("config.defaultLevel.set.title", l)).withEmoji(Emoji.fromUnicode("⚙️")),
                            TextDisplay.of(
                                    "### " + LangID.getStringByID("config.defaultLevel.title", l) + "\n" +
                                            "**" + LangID.getStringByID("config.defaultLevel.value", l).formatted(config.defLevel) + "**\n\n" +
                                            LangID.getStringByID("config.defaultLevel.description", l).formatted(config.defLevel)
                            )
                    ));
                case 1 -> {
                    String language;

                    if (config.lang == null) {
                        language = LangID.getStringByID("config.locale.auto", l);
                    } else {
                        language = switch(config.lang) {
                            case EN -> LangID.getStringByID("bot.language.en", l);
                            case JP -> LangID.getStringByID("bot.language.jp", l);
                            case KR -> LangID.getStringByID("bot.language.kr", l);
                            case ZH -> LangID.getStringByID("bot.language.zh", l);
                            case FR -> LangID.getStringByID("bot.language.fr", l);
                            case IT -> LangID.getStringByID("bot.language.it", l);
                            case ES -> LangID.getStringByID("bot.language.es", l);
                            case DE -> LangID.getStringByID("bot.language.de", l);
                            case TH -> LangID.getStringByID("bot.language.th", l);
                            case RU -> LangID.getStringByID("bot.language.ru", l);
                        };
                    }

                    String currentLocale = LangID.getStringByID("config.locale.value", l).formatted(language);

                    components.add(TextDisplay.of(
                            "### " + LangID.getStringByID("config.locale.title", l) + "\n" +
                                    "**" + currentLocale + "**"
                    ));

                    List<SelectOption> localeOptions = new ArrayList<>();

                    localeOptions.add(SelectOption.of(LangID.getStringByID("config.locale.auto", l), "auto").withDefault(config.lang == null));

                    for (CommonStatic.Lang.Locale locale : CommonStatic.Lang.supportedLanguage) {
                        Emoji emoji = Emoji.fromUnicode(StaticStore.langUnicode[locale.ordinal()]);

                        localeOptions.add(SelectOption.of(LangID.getStringByID("bot.language." + locale.code, l), locale.name()).withEmoji(emoji).withDefault(config.lang == locale));
                    }

                    components.add(ActionRow.of(StringSelectMenu.create("locale").addOptions(localeOptions).setPlaceholder(LangID.getStringByID("locale.selectList", l)).setRequiredRange(1, 1).build()));
                }
                case 2 -> {
                    Emoji compactEmbedSwitch;
                    String compactEmbedValue;
                    String compactEmbedDescription;

                    if (config.compact) {
                        compactEmbedSwitch = EmojiStore.SWITCHON;
                        compactEmbedValue = LangID.getStringByID("data.true", l);
                        compactEmbedDescription = LangID.getStringByID("config.compactEmbed.description.true", l);
                    } else {
                        compactEmbedSwitch = EmojiStore.SWITCHOFF;
                        compactEmbedValue = LangID.getStringByID("data.false", l);
                        compactEmbedDescription = LangID.getStringByID("config.compactEmbed.description.false", l);
                    }

                    components.add(Section.of(
                            Button.secondary("compact", compactEmbedValue).withEmoji(compactEmbedSwitch),
                            TextDisplay.of(
                                    "### " + LangID.getStringByID("config.compactEmbed.title", l) + "\n" +
                                            "**" + LangID.getStringByID("config.compactEmbed.value", l).formatted(compactEmbedValue) + "**\n\n" +
                                            compactEmbedDescription
                            )
                    ));
                }
                case 3 -> {
                    Emoji trueFormSwitch;
                    String trueFormValue;
                    String trueFormDescription;

                    if (config.trueForm) {
                        trueFormSwitch = EmojiStore.SWITCHON;
                        trueFormValue = LangID.getStringByID("data.true", l);
                        trueFormDescription = LangID.getStringByID("config.trueForm.description.true", l);
                    } else {
                        trueFormSwitch = EmojiStore.SWITCHOFF;
                        trueFormValue = LangID.getStringByID("data.false", l);
                        trueFormDescription = LangID.getStringByID("config.trueForm.description.false", l);
                    }

                    components.add(Section.of(
                            Button.secondary("trueForm", trueFormValue).withEmoji(trueFormSwitch),
                            TextDisplay.of(
                                    "### " + LangID.getStringByID("config.compactEmbed.title", l) + "\n" +
                                            "**" + LangID.getStringByID("config.compactEmbed.value", l).formatted(trueFormValue) + "**\n\n" +
                                            trueFormDescription
                            )
                    ));
                }
                case 4 -> {
                    Emoji frameSwitch;
                    String frameValue;
                    String frameDescription = LangID.getStringByID("config.defaultUnit.description", l);

                    if (config.useFrame) {
                        frameSwitch = Emoji.fromUnicode("🎞️");
                        frameValue = LangID.getStringByID("config.defaultUnit.frame", l);
                    } else {
                        frameSwitch = Emoji.fromUnicode("🕐");
                        frameValue = LangID.getStringByID("config.defaultUnit.second", l);
                    }

                    components.add(Section.of(
                            Button.secondary("unit", frameValue).withEmoji(frameSwitch),
                            TextDisplay.of(
                                    "### " + LangID.getStringByID("config.compactEmbed.title", l) + "\n" +
                                            "**" + LangID.getStringByID("config.compactEmbed.value", l).formatted(frameValue) + "**\n\n" +
                                            frameDescription
                            )
                    ));
                }
                case 5 -> {
                    Emoji treasureSwitch;
                    String treasureValue;
                    String treasureDescription;

                    if (config.treasure) {
                        treasureSwitch = EmojiStore.SWITCHON;
                        treasureValue = LangID.getStringByID("data.true", l);
                        treasureDescription = LangID.getStringByID("config.treasure.description.true", l);
                    } else {
                        treasureSwitch = EmojiStore.SWITCHOFF;
                        treasureValue = LangID.getStringByID("data.false", l);
                        treasureDescription = LangID.getStringByID("config.treasure.description.false", l);
                    }

                    components.add(Section.of(
                            Button.secondary("treasure", treasureValue).withEmoji(treasureSwitch),
                            TextDisplay.of(
                                    "### " + LangID.getStringByID("config.compactEmbed.title", l) + "\n" +
                                            "**" + LangID.getStringByID("config.compactEmbed.value", l).formatted(treasureValue) + "**\n\n" +
                                            treasureDescription
                            )
                    ));
                }
                case 6 -> components.add(ActionRow.of(Button.secondary("embed",LangID.getStringByID("config.commandList.button", l)).withEmoji(Emoji.fromUnicode("🎛️"))));
            }

            components.add(Separator.create(true, Separator.Spacing.LARGE));
        }

        int totalPage = getTotalPage(TOTAL_CONFIG, CONFIG_CHUNK);

        List<Button> buttons = new ArrayList<>();

        if(totalPage > 10) {
            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", LangID.getStringByID("ui.search.10Previous", l), EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0));
        }

        buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", LangID.getStringByID("ui.search.previous", l), EmojiStore.PREVIOUS).withDisabled(page - 1 < 0));
        buttons.add(Button.of(ButtonStyle.SECONDARY, "next", LangID.getStringByID("ui.search.next", l), EmojiStore.NEXT).withDisabled(page + 1 >= totalPage));

        if(totalPage > 10) {
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", LangID.getStringByID("ui.search.10Next", l), EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage));
        }

        components.add(ActionRow.of(buttons));

        components.add(Separator.create(false, Separator.Spacing.SMALL));

        components.add(ActionRow.of(
                Button.primary("confirm", LangID.getStringByID("ui.button.confirm", l)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", l)).withEmoji(EmojiStore.CROSS)
        ));

        return Container.of(components);
    }
}
