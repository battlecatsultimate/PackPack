package mandarin.packpack.supporter.server.holder.component.config.guild;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.LevelModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ConfigCommandHolder extends ServerConfigHolder {
    private static final int PAGE_SIZE = 3;

    private int page = 0;

    public ConfigCommandHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull IDHolder holder, @Nonnull IDHolder backup, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "level" -> {
                TextInput input = TextInput.create("level", LangID.getStringByID("config.defaultLevel.set.inputTagName", holder.config.lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("config.defaultLevel.set.placeholder", holder.config.lang))
                        .setRequiredRange(1, 2)
                        .setRequired(true)
                        .setValue(String.valueOf(holder.config.defLevel))
                        .build();

                Modal modal = Modal.create("level", LangID.getStringByID("config.defaultLevel.set.tagName", holder.config.lang))
                        .addComponents(ActionRow.of(input))
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new LevelModalHolder(getAuthorMessage(), userID, channelID, message, holder.config, this::applyResult, lang));
            }
            case "unit" -> {
                holder.config.useFrame = !holder.config.useFrame;

                applyResult(event);
            }
            case "compact" -> {
                holder.config.compact = !holder.config.compact;

                applyResult(event);
            }
            case "forceCompact" -> {
                holder.forceCompact = !holder.forceCompact;

                applyResult(event);
            }
            case "trueForm" -> {
                holder.config.trueForm = !holder.config.trueForm;

                applyResult(event);
            }
            case "treasure" -> {
                holder.config.treasure = !holder.config.treasure;

                applyResult(event);
            }
            case "forceTreasure" -> {
                holder.forceFullTreasure = !backup.forceFullTreasure;

                applyResult(event);
            }
            case "embed" -> connectTo(event, new ConfigEmbedListHolder(getAuthorMessage(), userID, channelID, message, holder, backup, lang));
            case "prev" -> {
                page--;

                applyResult(event);
            }
            case "next" -> {
                page++;

                applyResult(event);
            }
            case "back" -> goBack(event);
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
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) {
        applyResult(event);
    }

    private void applyResult(IMessageEditCallback event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private void applyResult(ModalInteractionEvent event) {
        event.deferEdit()
                .setContent(getContents())
                .setComponents(getComponents())
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    private String getContents() {
        switch (page) {
            case 0 -> {
                String unit;
                String unitExample;
                Emoji unitEmoji;

                if (holder.config.useFrame) {
                    unit = LangID.getStringByID("config.defaultUnit.frame", lang);
                    unitExample = LangID.getStringByID("serverConfig.command.documentation.defaultUnit.example.frame", lang);
                    unitEmoji = Emoji.fromUnicode("📏");
                } else {
                    unit = LangID.getStringByID("config.defaultUnit.second", lang);
                    unitExample = LangID.getStringByID("serverConfig.command.documentation.defaultUnit.example.second", lang);
                    unitEmoji = Emoji.fromUnicode("⏱️");
                }

                return LangID.getStringByID("serverConfig.command.documentation.title", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.defaultLevel.title", lang).formatted(EmojiStore.LEVEL, holder.config.defLevel) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.defaultLevel.description", lang).formatted(holder.config.defLevel, holder.config.defLevel) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.defaultUnit.title", lang).formatted(unitEmoji, unit) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.defaultUnit.description", lang).formatted(unit, unitExample);
            }
            case 1 -> {
                String compacted;

                if (holder.config.compact) {
                    compacted = LangID.getStringByID("data.true", lang);
                } else {
                    compacted = LangID.getStringByID("data.false", lang);
                }

                String forceCompacted;
                Emoji forceCompactedSwitch;

                if (holder.forceCompact) {
                    forceCompacted = LangID.getStringByID("data.true", lang);
                    forceCompactedSwitch = EmojiStore.SWITCHON;
                } else {
                    forceCompacted = LangID.getStringByID("data.false", lang);
                    forceCompactedSwitch = EmojiStore.SWITCHOFF;
                }

                String trueFormSearch;

                if (holder.config.trueForm) {
                    trueFormSearch = LangID.getStringByID("data.true", lang);
                } else {
                    trueFormSearch = LangID.getStringByID("data.false", lang);
                }

                return LangID.getStringByID("serverConfig.command.documentation.title", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.compactEmbed.title", lang).formatted(EmojiStore.COMPRESS, compacted) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.compactEmbed.description", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.forceCompact.title", lang).formatted(forceCompactedSwitch, forceCompacted) +"\n" +
                        LangID.getStringByID("serverConfig.command.documentation.forceCompact.description", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.trueFormSearch.title", lang).formatted(Emoji.fromUnicode("🔎"), trueFormSearch) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.trueFormSearch.description", lang);
            }
            case 2 -> {
                String treasure;

                if (holder.config.treasure) {
                    treasure = LangID.getStringByID("data.true", lang);
                } else {
                    treasure = LangID.getStringByID("data.false", lang);
                }

                String forceTreasure;
                Emoji forceTreasureSwitch;

                if (holder.forceFullTreasure) {
                    forceTreasure = LangID.getStringByID("data.true", lang);
                    forceTreasureSwitch = EmojiStore.SWITCHON;
                } else {
                    forceTreasure = LangID.getStringByID("data.false", lang);
                    forceTreasureSwitch = EmojiStore.SWITCHOFF;
                }

                return LangID.getStringByID("serverConfig.command.documentation.title", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.treasure.title", lang).formatted(EmojiStore.TREASURE_RADAR, treasure) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.treasure.description", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.forceTreasure.title", lang).formatted(forceTreasureSwitch, forceTreasure) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.forceTreasure.description", lang) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.embed.title", lang).formatted(Emoji.fromUnicode("🎛️").getFormatted()) + "\n" +
                        LangID.getStringByID("serverConfig.command.documentation.embed.description", lang);
            }
        }

        throw new IndexOutOfBoundsException("Invalid range of command setting config. Page index out of bound : Page = %d, Size = %d".formatted(page, PAGE_SIZE));
    }

    private List<MessageTopLevelComponent> getComponents() {
        List<MessageTopLevelComponent> result = new ArrayList<>();

        switch (page) {
            case 0 -> {
                String unit;
                Emoji unitEmoji;

                if (holder.config.useFrame) {
                    unit = LangID.getStringByID("config.defaultUnit.frame", lang);
                    unitEmoji = Emoji.fromUnicode("📏");
                } else {
                    unit = LangID.getStringByID("config.defaultUnit.second", lang);
                    unitEmoji = Emoji.fromUnicode("⏱️");
                }

                result.add(ActionRow.of(Button.secondary("level", LangID.getStringByID("serverConfig.command.button.defaultLevel", lang).formatted(holder.config.defLevel)).withEmoji(EmojiStore.LEVEL)));
                result.add(ActionRow.of(Button.secondary("unit", LangID.getStringByID("serverConfig.command.button.defaultUnit", lang).formatted(unit)).withEmoji(unitEmoji)));
            }
            case 1 -> {
                Emoji compacted;
                String compactedText;

                if (holder.config.compact) {
                    compacted = EmojiStore.SWITCHON;
                    compactedText = LangID.getStringByID("data.true", lang);
                } else {
                    compacted = EmojiStore.SWITCHOFF;
                    compactedText = LangID.getStringByID("data.false", lang);
                }

                Emoji forceCompactSwitch;

                if (holder.forceCompact) {
                    forceCompactSwitch = EmojiStore.SWITCHON;
                } else {
                    forceCompactSwitch = EmojiStore.SWITCHOFF;
                }

                Emoji trueFormSwitch;

                if (holder.config.trueForm) {
                    trueFormSwitch = EmojiStore.SWITCHON;
                } else {
                    trueFormSwitch = EmojiStore.SWITCHOFF;
                }

                result.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("serverConfig.command.button.compactEmbed", lang).formatted(compactedText)).withEmoji(compacted)));
                result.add(ActionRow.of(Button.secondary("forceCompact", LangID.getStringByID("serverConfig.command.button.forceCompact", lang)).withEmoji(forceCompactSwitch)));
                result.add(ActionRow.of(Button.secondary("trueForm", LangID.getStringByID("serverConfig.command.button.trueFormSearch", lang)).withEmoji(trueFormSwitch)));
            }
            case 2 -> {
                Emoji treasureSwitch;

                if (holder.config.treasure) {
                    treasureSwitch = EmojiStore.SWITCHON;
                } else {
                    treasureSwitch = EmojiStore.SWITCHOFF;
                }

                Emoji forceTreasureSwitch;

                if (holder.forceFullTreasure) {
                    forceTreasureSwitch = EmojiStore.SWITCHON;
                } else {
                    forceTreasureSwitch = EmojiStore.SWITCHOFF;
                }

                result.add(ActionRow.of(Button.secondary("treasure", LangID.getStringByID("serverConfig.command.button.treasure", lang)).withEmoji(treasureSwitch)));
                result.add(ActionRow.of(Button.secondary("forceTreasure", LangID.getStringByID("serverConfig.command.button.forceTreasure", lang)).withEmoji(forceTreasureSwitch)));
                result.add(ActionRow.of(Button.secondary("embed",LangID.getStringByID("serverConfig.command.button.embed", lang)).withEmoji(Emoji.fromUnicode("🎛️"))));
            }
        }

        result.add(ActionRow.of(
                Button.secondary("prev", LangID.getStringByID("ui.search.previous", lang)).withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", LangID.getStringByID("ui.search.next", lang)).withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= PAGE_SIZE)
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("ui.button.back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}