package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.modal.TreasureModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class TreasureButtonHolder extends ComponentHolder {
    private final TreasureHolder treasure;
    private final TreasureHolder backup;

    public TreasureButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull TreasureHolder treasure, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.treasure = treasure;
        backup = this.treasure.copy();

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "basic" -> {
                TextInput research = buildTextInput("research", "data.treasure.upgrades.research", "treasure.set.level.research", false,  1, TreasureHolder.basicMax[TreasureHolder.L_RESEARCH], String.valueOf(treasure.basic[TreasureHolder.L_RESEARCH]));
                TextInput account = buildTextInput("account", "data.treasure.upgrades.accountant", "treasure.set.level.accountant", false, 1, TreasureHolder.basicMax[TreasureHolder.L_ACCOUNTANT], String.valueOf(treasure.basic[TreasureHolder.L_ACCOUNTANT]));
                TextInput study = buildTextInput("study", "data.treasure.upgrades.study", "treasure.set.level.study", false, 1, TreasureHolder.basicMax[TreasureHolder.L_STUDY], String.valueOf(treasure.basic[TreasureHolder.L_STUDY]));

                Modal modal = Modal.create("basic", LangID.getStringByID("data.treasure.upgrades.title", lang))
                        .addActionRow(research)
                        .addActionRow(account)
                        .addActionRow(study)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), userID, channelID, message, treasure, lang, TreasureModalHolder.TREASURE.BASIC, this::applyResult));
            }
            case "eoc" -> {
                TextInput research = buildTextInput("research", "data.treasure.eoc.research.ui", "treasure.set.treasure.research", true, 0, TreasureHolder.eocMax[TreasureHolder.T_RESEARCH], String.valueOf(treasure.eoc[TreasureHolder.T_RESEARCH]));
                TextInput study = buildTextInput("study", "data.treasure.eoc.study.ui", "treasure.set.treasure.study.eoc", true, 0, TreasureHolder.eocMax[TreasureHolder.T_STUDY], String.valueOf(treasure.eoc[TreasureHolder.T_STUDY]));
                TextInput account = buildTextInput("account", "data.treasure.eoc.accountant.ui", "treasure.set.treasure.accountant", true, 0, TreasureHolder.eocMax[TreasureHolder.T_ACCOUNTANT], String.valueOf(treasure.eoc[TreasureHolder.T_ACCOUNTANT]));
                TextInput health = buildTextInput("health", "data.treasure.eoc.health.ui", "treasure.set.treasure.health", true, 0, TreasureHolder.eocMax[TreasureHolder.T_HEALTH], String.valueOf(treasure.eoc[TreasureHolder.T_HEALTH]));
                TextInput attack = buildTextInput("attack", "data.treasure.eoc.damage.ui", "treasure.set.treasure.attack", true, 0, TreasureHolder.eocMax[TreasureHolder.T_ATTACK], String.valueOf(treasure.eoc[TreasureHolder.T_ATTACK]));

                Modal modal = Modal.create("eoc", LangID.getStringByID("data.treasure.eoc.title", lang))
                        .addActionRow(research)
                        .addActionRow(study)
                        .addActionRow(account)
                        .addActionRow(health)
                        .addActionRow(attack)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), userID, channelID, message, treasure, lang, TreasureModalHolder.TREASURE.EOC, this::applyResult));
            }
            case "itf" -> {
                TextInput crystal = buildTextInput("crystal", "data.treasure.itf.crystal", "treasure.set.treasure.itfCrystal", true, 0, TreasureHolder.itfMax[TreasureHolder.T_ITF_CRYSTAL], String.valueOf(treasure.itf[TreasureHolder.T_ITF_CRYSTAL]));
                TextInput black = buildTextInput("black", "data.treasure.itf.black.ui", "treasure.set.treasure.black", true, 0, TreasureHolder.itfMax[TreasureHolder.T_BLACK], String.valueOf(treasure.itf[TreasureHolder.T_BLACK]));
                TextInput red = buildTextInput("red", "data.treasure.itf.red.ui", "treasure.set.treasure.red", true, 0, TreasureHolder.itfMax[TreasureHolder.T_RED], String.valueOf(treasure.itf[TreasureHolder.T_RED]));
                TextInput floating = buildTextInput("float", "data.treasure.itf.floating.ui", "treasure.set.treasure.floating", true, 0, TreasureHolder.itfMax[TreasureHolder.T_FLOAT], String.valueOf(treasure.itf[TreasureHolder.T_FLOAT]));
                TextInput angel = buildTextInput("angel", "data.treasure.itf.angel.ui", "treasure.set.treasure.angel", true, 0, TreasureHolder.itfMax[TreasureHolder.T_ANGEL], String.valueOf(treasure.itf[TreasureHolder.T_ANGEL]));

                Modal modal = Modal.create("itf", LangID.getStringByID("data.treasure.itf.title", lang))
                        .addActionRow(crystal)
                        .addActionRow(black)
                        .addActionRow(red)
                        .addActionRow(floating)
                        .addActionRow(angel)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), userID, channelID, message, treasure, lang, TreasureModalHolder.TREASURE.ITF, this::applyResult));
            }
            case "cotc" -> {
                TextInput crystal = buildTextInput("crystal", "data.treasure.cotc.crystal", "treasure.set.treasure.cotcCrystal", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_COTC_CRYSTAL], String.valueOf(treasure.cotc[TreasureHolder.T_COTC_CRYSTAL]));
                TextInput metal = buildTextInput("metal", "data.treasure.cotc.metal.ui", "treasure.set.treasure.metal", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_METAL], String.valueOf(treasure.cotc[TreasureHolder.T_METAL]));
                TextInput zombie = buildTextInput("zombie", "data.treasure.cotc.zombie.ui", "treasure.set.treasure.zombie", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_ZOMBIE], String.valueOf(treasure.cotc[TreasureHolder.T_ZOMBIE]));
                TextInput alien = buildTextInput("alien", "data.treasure.cotc.alien.ui", "treasure.set.treasure.alien", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_ALIEN], String.valueOf(treasure.cotc[TreasureHolder.T_ALIEN]));
                TextInput study = buildTextInput("study", "data.treasure.cotc.study.ui", "treasure.set.treasure.study.cotc", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_STUDY2], String.valueOf(treasure.cotc[TreasureHolder.T_STUDY2]));

                Modal modal = Modal.create("cotc", LangID.getStringByID("data.treasure.cotc.title", lang))
                        .addActionRow(crystal)
                        .addActionRow(metal)
                        .addActionRow(zombie)
                        .addActionRow(alien)
                        .addActionRow(study)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), userID, channelID, message, treasure, lang, TreasureModalHolder.TREASURE.COTC, this::applyResult));
            }
            case "confirm" -> {
                event.editMessage(LangID.getStringByID("treasure.finished", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setComponents()
                        .queue();

                StaticStore.treasure.put(userID, treasure);

                end(true);
            }
            case "cancel" -> {
                event.editMessage(LangID.getStringByID("treasure.canceled", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setComponents()
                        .queue();

                TreasureHolder previous = StaticStore.treasure.get(userID);

                if(previous != null) {
                    StaticStore.treasure.put(userID, backup);
                }

                end(true);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("treasure.expired", lang))
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents()
                .queue();

        TreasureHolder previous = StaticStore.treasure.get(userID);

        if(previous != null) {
            StaticStore.treasure.put(userID, backup);
        }
    }

    private TextInput buildTextInput(String id, String titleID, String placeholderID, boolean percent, int rangeStart, int rangeEnd, @Nullable String defaultValue) {
        return TextInput.create(id, LangID.getStringByID(titleID, lang) + String.format(LangID.getStringByID(percent ? "treasure.range.percent" : "treasure.range.level", lang), rangeStart, rangeEnd), TextInputStyle.SHORT)
                .setPlaceholder(LangID.getStringByID(placeholderID, lang))
                .setRequired(true)
                .setRequiredRange(1, String.valueOf(rangeEnd).length())
                .setValue(defaultValue)
                .build();
    }

    private void applyResult() {
        attachUIComponents(message.editMessage(generateText(treasure))).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
    }

    private String generateText(TreasureHolder treasure) {
        StringBuilder generator = new StringBuilder();

        generator.append("**")
                .append(LangID.getStringByID("data.treasure.upgrades.title", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.basicText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.basicText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure.value.level", lang), treasure.basic[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data.treasure.eoc.title", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.eocText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.eocText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure.value.percent", lang), treasure.eoc[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data.treasure.itf.title", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.itfText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.itfText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure.value.percent", lang), treasure.itf[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data.treasure.cotc.title", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.cotcText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.cotcText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure.value.percent", lang), treasure.cotc[i]))
                    .append("\n");
        }

        return generator.toString();
    }

    private MessageEditAction attachUIComponents(MessageEditAction a) {
        return a.setComponents(
                ActionRow.of(Button.secondary("basic", LangID.getStringByID("treasure.adjust.basicLevels", lang)).withEmoji(EmojiStore.ORB)),
                ActionRow.of(Button.secondary("eoc", LangID.getStringByID("treasure.adjust.EoC", lang)).withEmoji(EmojiStore.DOGE)),
                ActionRow.of(Button.secondary("itf", LangID.getStringByID("treasure.adjust.ItF", lang)).withEmoji(EmojiStore.SHIBALIEN)),
                ActionRow.of(Button.secondary("cotc", LangID.getStringByID("treasure.adjust.CotC", lang)).withEmoji(EmojiStore.SHIBALIENELITE)),
                ActionRow.of(Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)), Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)))
        );
    }
}
