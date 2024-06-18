package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder;
import mandarin.packpack.supporter.server.holder.modal.LevelModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ConfigCommandHolder extends ServerConfigHolder {
    private static final int PAGE_SIZE = 3;

    private int page = 0;

    public ConfigCommandHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, @NotNull IDHolder holder, @NotNull IDHolder backup, int lang) {
        super(author, channelID, message, holder, backup, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "level" -> {
                TextInput input = TextInput.create("level", LangID.getStringByID("config_levelsubject", holder.config.lang), TextInputStyle.SHORT)
                        .setPlaceholder(LangID.getStringByID("config_levelplace", holder.config.lang))
                        .setRequiredRange(1, 2)
                        .setRequired(true)
                        .setValue(String.valueOf(holder.config.defLevel))
                        .build();

                Modal modal = Modal.create("level", LangID.getStringByID("config_leveltitle", holder.config.lang))
                        .addActionRow(input)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new LevelModalHolder(getAuthorMessage(), message, channelID, holder.config, this::applyResult));
            }
            case "unit" -> {
                holder.config.useFrame = !holder.config.useFrame;

                applyResult(event);
            }
            case "extra" -> {
                holder.config.extra = !holder.config.extra;

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
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onConnected(@NotNull GenericComponentInteractionCreateEvent event) {
        applyResult(event);
    }

    private void applyResult(GenericComponentInteractionCreateEvent event) {
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
                    unit = LangID.getStringByID("config_frame", lang);
                    unitExample = LangID.getStringByID("sercon_commandunitexamf", lang);
                    unitEmoji = Emoji.fromUnicode("📏");
                } else {
                    unit = LangID.getStringByID("config_second", lang);
                    unitExample = LangID.getStringByID("sercon_commandunitexams", lang);
                    unitEmoji = Emoji.fromUnicode("⏱️");
                }

                String extra;

                if (holder.config.extra) {
                    extra = LangID.getStringByID("data_true", lang);
                } else {
                    extra = LangID.getStringByID("data_false", lang);
                }

                return LangID.getStringByID("sercon_commandtitle", lang) + "\n" +
                        LangID.getStringByID("sercon_commandlvtit", lang).formatted(EmojiStore.LEVEL, holder.config.defLevel) + "\n" +
                        LangID.getStringByID("sercon_commandlvdesc", lang).formatted(holder.config.defLevel, holder.config.defLevel) + "\n" +
                        LangID.getStringByID("sercon_commandunittit", lang).formatted(unitEmoji, unit) + "\n" +
                        LangID.getStringByID("sercon_commandunitdesc", lang).formatted(unit, unitExample) + "\n" +
                        LangID.getStringByID("sercon_commandextratit", lang).formatted(EmojiStore.INFORMATION, extra) + "\n" +
                        LangID.getStringByID("sercon_commandextradesc", lang);
            }
            case 1 -> {
                String compacted;

                if (holder.config.compact) {
                    compacted = LangID.getStringByID("data_true", lang);
                } else {
                    compacted = LangID.getStringByID("data_false", lang);
                }

                String forceCompacted;
                Emoji forceCompactedSwitch;

                if (holder.forceCompact) {
                    forceCompacted = LangID.getStringByID("data_true", lang);
                    forceCompactedSwitch = EmojiStore.SWITCHON;
                } else {
                    forceCompacted = LangID.getStringByID("data_false", lang);
                    forceCompactedSwitch = EmojiStore.SWITCHOFF;
                }

                String trueFormSearch;

                if (holder.config.trueForm) {
                    trueFormSearch = LangID.getStringByID("data_true", lang);
                } else {
                    trueFormSearch = LangID.getStringByID("data_false", lang);
                }

                return LangID.getStringByID("sercon_commandtitle", lang) + "\n" +
                        LangID.getStringByID("sercon_commandcompacttit", lang).formatted(EmojiStore.COMPRESS, compacted) + "\n" +
                        LangID.getStringByID("sercon_commandcompactdesc", lang) + "\n" +
                        LangID.getStringByID("sercon_commandforcomtit", lang).formatted(forceCompactedSwitch, forceCompacted) +"\n" +
                        LangID.getStringByID("sercon_commandforcomdesc", lang) + "\n" +
                        LangID.getStringByID("sercon_commandtruetit", lang).formatted(Emoji.fromUnicode("🔎"), trueFormSearch) + "\n" +
                        LangID.getStringByID("sercon_commandtruedesc", lang);
            }
            case 2 -> {
                String treasure;

                if (holder.config.treasure) {
                    treasure = LangID.getStringByID("data_true", lang);
                } else {
                    treasure = LangID.getStringByID("data_false", lang);
                }

                String forceTreasure;
                Emoji forceTreasureSwitch;

                if (holder.forceFullTreasure) {
                    forceTreasure = LangID.getStringByID("data_true", lang);
                    forceTreasureSwitch = EmojiStore.SWITCHON;
                } else {
                    forceTreasure = LangID.getStringByID("data_false", lang);
                    forceTreasureSwitch = EmojiStore.SWITCHOFF;
                }

                return LangID.getStringByID("sercon_commandtitle", lang) + "\n" +
                        LangID.getStringByID("sercon_commandtreasuretit", lang).formatted(EmojiStore.TREASURE_RADAR, treasure) + "\n" +
                        LangID.getStringByID("sercon_commandtreasuredesc", lang) + "\n" +
                        LangID.getStringByID("sercon_commandfortrtit", lang).formatted(forceTreasureSwitch, forceTreasure) + "\n" +
                        LangID.getStringByID("sercon_commandfortrdesc", lang);
            }
        }

        throw new IndexOutOfBoundsException("Invalid range of command setting config. Page index out of bound : Page = %d, Size = %d".formatted(page, PAGE_SIZE));
    }

    private List<LayoutComponent> getComponents() {
        List<LayoutComponent> result = new ArrayList<>();

        switch (page) {
            case 0 -> {
                String unit;
                Emoji unitEmoji;

                if (holder.config.useFrame) {
                    unit = LangID.getStringByID("config_frame", lang);
                    unitEmoji = Emoji.fromUnicode("📏");
                } else {
                    unit = LangID.getStringByID("config_second", lang);
                    unitEmoji = Emoji.fromUnicode("⏱️");
                }

                Emoji extra;

                if (holder.config.extra) {
                    extra = EmojiStore.SWITCHON;
                } else {
                    extra = EmojiStore.SWITCHOFF;
                }

                result.add(ActionRow.of(Button.secondary("level", LangID.getStringByID("sercon_commandlvbutton", lang).formatted(holder.config.defLevel)).withEmoji(EmojiStore.LEVEL)));
                result.add(ActionRow.of(Button.secondary("unit", LangID.getStringByID("sercon_commandunitbutton", lang).formatted(unit)).withEmoji(unitEmoji)));
                result.add(ActionRow.of(Button.secondary("extra", LangID.getStringByID("sercon_commandextrabutton", lang)).withEmoji(extra)));

            }
            case 1 -> {
                Emoji extraSwitch;

                if (holder.config.extra) {
                    extraSwitch = EmojiStore.SWITCHON;
                } else {
                    extraSwitch = EmojiStore.SWITCHOFF;
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

                result.add(ActionRow.of(Button.secondary("compact", LangID.getStringByID("sercon_commandcompactbutton", lang)).withEmoji(extraSwitch)));
                result.add(ActionRow.of(Button.secondary("forceCompact", LangID.getStringByID("sercon_commandforcombutton", lang)).withEmoji(forceCompactSwitch)));
                result.add(ActionRow.of(Button.secondary("trueForm", LangID.getStringByID("sercon_commandtruebutton", lang)).withEmoji(trueFormSwitch)));
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

                result.add(ActionRow.of(Button.secondary("treasure", LangID.getStringByID("sercon_commandtreasurebutton", lang)).withEmoji(treasureSwitch)));
                result.add(ActionRow.of(Button.secondary("forceTreasure", LangID.getStringByID("sercon_commandfortrbutton", lang)).withEmoji(forceTreasureSwitch)));
            }
        }

        result.add(ActionRow.of(
                Button.secondary("prev", LangID.getStringByID("search_prev", lang)).withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", LangID.getStringByID("search_next", lang)).withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= PAGE_SIZE)
        ));

        result.add(ActionRow.of(
                Button.secondary("back", LangID.getStringByID("button_back", lang)).withEmoji(EmojiStore.BACK),
                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
        ));

        return result;
    }
}