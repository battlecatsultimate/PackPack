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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class TreasureButtonHolder extends ComponentHolder {
    private final TreasureHolder treasure;
    private final TreasureHolder backup;

    public TreasureButtonHolder(@NotNull Message author, @NotNull Message msg, @NotNull String channelID, @NotNull TreasureHolder treasure, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        this.treasure = treasure;
        backup = this.treasure.copy();

        registerAutoFinish(this, msg, "treasure_expire", FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "basic" -> {
                TextInput research = buildTextInput("research", "data_lresearch", "treasure_lresearchpl", false,  1, TreasureHolder.basicMax[TreasureHolder.L_RESEARCH], String.valueOf(treasure.basic[TreasureHolder.L_RESEARCH]));
                TextInput account = buildTextInput("account", "data_laccount", "treasure_laccountpl", false, 1, TreasureHolder.basicMax[TreasureHolder.L_ACCOUNTANT], String.valueOf(treasure.basic[TreasureHolder.L_ACCOUNTANT]));
                TextInput study = buildTextInput("study", "data_lstudy", "treasure_lstudypl", false, 1, TreasureHolder.basicMax[TreasureHolder.L_STUDY], String.valueOf(treasure.basic[TreasureHolder.L_STUDY]));

                Modal modal = Modal.create("basic", LangID.getStringByID("data_tbasic", lang))
                        .addActionRow(research)
                        .addActionRow(account)
                        .addActionRow(study)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), channelID, message, treasure, lang, TreasureModalHolder.TREASURE.BASIC, this::applyResult));
            }
            case "eoc" -> {
                TextInput research = buildTextInput("research", "data_tresearchs", "treasure_tresearchpl", true, 0, TreasureHolder.eocMax[TreasureHolder.T_RESEARCH], String.valueOf(treasure.eoc[TreasureHolder.T_RESEARCH]));
                TextInput study = buildTextInput("study", "data_tstudys", "treasure_tstudypl", true, 0, TreasureHolder.eocMax[TreasureHolder.T_STUDY], String.valueOf(treasure.eoc[TreasureHolder.T_STUDY]));
                TextInput account = buildTextInput("account", "data_taccounts", "treasure_taccountpl", true, 0, TreasureHolder.eocMax[TreasureHolder.T_ACCOUNTANT], String.valueOf(treasure.eoc[TreasureHolder.T_ACCOUNTANT]));
                TextInput health = buildTextInput("health", "data_thealths", "treasure_thealth", true, 0, TreasureHolder.eocMax[TreasureHolder.T_HEALTH], String.valueOf(treasure.eoc[TreasureHolder.T_HEALTH]));
                TextInput attack = buildTextInput("attack", "data_tattacks", "treasure_tattackpl", true, 0, TreasureHolder.eocMax[TreasureHolder.T_ATTACK], String.valueOf(treasure.eoc[TreasureHolder.T_ATTACK]));

                Modal modal = Modal.create("eoc", LangID.getStringByID("data_teoc", lang))
                        .addActionRow(research)
                        .addActionRow(study)
                        .addActionRow(account)
                        .addActionRow(health)
                        .addActionRow(attack)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), channelID, message, treasure, lang, TreasureModalHolder.TREASURE.EOC, this::applyResult));
            }
            case "itf" -> {
                TextInput crystal = buildTextInput("crystal", "data_citf", "treasure_citfpl", true, 0, TreasureHolder.itfMax[TreasureHolder.T_ITF_CRYSTAL], String.valueOf(treasure.itf[TreasureHolder.T_ITF_CRYSTAL]));
                TextInput black = buildTextInput("black", "data_tblacks", "treasure_tblackpl", true, 0, TreasureHolder.itfMax[TreasureHolder.T_BLACK], String.valueOf(treasure.itf[TreasureHolder.T_BLACK]));
                TextInput red = buildTextInput("red", "data_treds", "treasure_tredpl", true, 0, TreasureHolder.itfMax[TreasureHolder.T_RED], String.valueOf(treasure.itf[TreasureHolder.T_RED]));
                TextInput floating = buildTextInput("float", "data_tfloats", "treasure_tfloatpl", true, 0, TreasureHolder.itfMax[TreasureHolder.T_FLOAT], String.valueOf(treasure.itf[TreasureHolder.T_FLOAT]));
                TextInput angel = buildTextInput("angel", "data_tangels", "treasure_tangelpl", true, 0, TreasureHolder.itfMax[TreasureHolder.T_ANGEL], String.valueOf(treasure.itf[TreasureHolder.T_ANGEL]));

                Modal modal = Modal.create("itf", LangID.getStringByID("data_titf", lang))
                        .addActionRow(crystal)
                        .addActionRow(black)
                        .addActionRow(red)
                        .addActionRow(floating)
                        .addActionRow(angel)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), channelID, message, treasure, lang, TreasureModalHolder.TREASURE.ITF, this::applyResult));
            }
            case "cotc" -> {
                TextInput crystal = buildTextInput("crystal", "data_ccotc", "treasure_ccotcpl", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_COTC_CRYSTAL], String.valueOf(treasure.cotc[TreasureHolder.T_COTC_CRYSTAL]));
                TextInput metal = buildTextInput("metal", "data_tmetals", "treasure_tmetalpl", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_METAL], String.valueOf(treasure.cotc[TreasureHolder.T_METAL]));
                TextInput zombie = buildTextInput("zombie", "data_tzombies", "treasure_tzombiepl", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_ZOMBIE], String.valueOf(treasure.cotc[TreasureHolder.T_ZOMBIE]));
                TextInput alien = buildTextInput("alien", "data_taliens", "treasure_talienpl", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_ALIEN], String.valueOf(treasure.cotc[TreasureHolder.T_ALIEN]));
                TextInput study = buildTextInput("study", "data_tstudy2s", "treaasure_tstudy2pl", true, 0, TreasureHolder.cotcMax[TreasureHolder.T_STUDY2], String.valueOf(treasure.cotc[TreasureHolder.T_STUDY2]));

                Modal modal = Modal.create("cotc", LangID.getStringByID("data_tcotc", lang))
                        .addActionRow(crystal)
                        .addActionRow(metal)
                        .addActionRow(zombie)
                        .addActionRow(alien)
                        .addActionRow(study)
                        .build();

                event.replyModal(modal).queue();

                StaticStore.putHolder(userID, new TreasureModalHolder(getAuthorMessage(), channelID, message, treasure, lang, TreasureModalHolder.TREASURE.COTC, this::applyResult));
            }
            case "confirm" -> {
                event.editMessage(LangID.getStringByID("treasure_done", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setComponents()
                        .queue();

                StaticStore.treasure.put(userID, treasure);

                expired = true;

                expire(userID);
            }
            case "cancel" -> {
                event.editMessage(LangID.getStringByID("treasure_cancel", lang))
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .setComponents()
                        .queue();

                TreasureHolder previous = StaticStore.treasure.get(userID);

                if(previous != null) {
                    StaticStore.treasure.put(userID, backup);
                }

                expired = true;

                expire(userID);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        if(!expired) {
            expired = true;

            message.editMessage(LangID.getStringByID("treasure_expire", lang))
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .setComponents()
                    .queue();

            TreasureHolder previous = StaticStore.treasure.get(id);

            if(previous != null) {
                StaticStore.treasure.put(id, backup);
            }
        }
    }

    private TextInput buildTextInput(String id, String titleID, String placeholderID, boolean percent, int rangeStart, int rangeEnd, @Nullable String defaultValue) {
        return TextInput.create(id, LangID.getStringByID(titleID, lang) + String.format(LangID.getStringByID(percent ? "treasure_pcrange" : "treasure_lvrange", lang), rangeStart, rangeEnd), TextInputStyle.SHORT)
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
                .append(LangID.getStringByID("data_tbasic", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.basicText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.basicText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_level", lang), treasure.basic[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_teoc", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.eocText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.eocText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.eoc[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_titf", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.itfText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.itfText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.itf[i]))
                    .append("\n");
        }

        generator.append("\n**")
                .append(LangID.getStringByID("data_tcotc", lang))
                .append("**\n\n");

        for(int i = 0; i < TreasureHolder.cotcText.length; i++) {
            generator.append(LangID.getStringByID(TreasureHolder.cotcText[i], lang))
                    .append(String.format(LangID.getStringByID("treasure_percent", lang), treasure.cotc[i]))
                    .append("\n");
        }

        return generator.toString();
    }

    private MessageEditAction attachUIComponents(MessageEditAction a) {
        return a.setComponents(
                ActionRow.of(Button.secondary("basic", LangID.getStringByID("treasure_basic", lang)).withEmoji(EmojiStore.ORB)),
                ActionRow.of(Button.secondary("eoc", LangID.getStringByID("treasure_eoc", lang)).withEmoji(EmojiStore.DOGE)),
                ActionRow.of(Button.secondary("itf", LangID.getStringByID("treasure_itf", lang)).withEmoji(EmojiStore.SHIBALIEN)),
                ActionRow.of(Button.secondary("cotc", LangID.getStringByID("treasure_cotc", lang)).withEmoji(EmojiStore.SHIBALIENELITE)),
                ActionRow.of(Button.success("confirm", LangID.getStringByID("button_confirm", lang)), Button.danger("cancel", LangID.getStringByID("button_cancel", lang)))
        );
    }
}
