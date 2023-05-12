package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.segment.ModalHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TreasureModalHolder extends ModalHolder {
    public enum TREASURE {
        BASIC,
        EOC,
        ITF,
        COTC
    }

    private final TreasureHolder treasure;
    private final int lang;

    private final TREASURE type;

    private final Runnable editor;

    public TreasureModalHolder(@NotNull Message author, @NotNull String channelID, @NotNull String messageID, @NotNull TreasureHolder treasure, int lang, TREASURE type, Runnable editor) {
        super(author, channelID, messageID);

        this.treasure = treasure;
        this.lang = lang;

        this.type = type;

        this.editor = editor;
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }

    @Override
    public void onEvent(ModalInteractionEvent event) {
        StringBuilder result = new StringBuilder();
        String key = null;
        boolean[] done = new boolean[1];

        switch (type) {
            case BASIC -> {
                String[] values = prepareValues(event, "research", "account", "study");

                String[] texts = prepareTexts(
                        TreasureHolder.basicText[TreasureHolder.L_RESEARCH],
                        TreasureHolder.basicText[TreasureHolder.L_ACCOUNTANT],
                        TreasureHolder.basicText[TreasureHolder.L_STUDY]
                );

                done = new boolean[values.length];

                for(int i = 0; i < values.length; i++) {
                    if(!StaticStore.isNumeric(values[i])) {
                        result.append(String.format(LangID.getStringByID("treasure_notnum", lang), texts[i]))
                                .append("\n\n");
                    } else {
                        int v = StaticStore.safeParseInt(values[i]);

                        if(v < 1 || v > TreasureHolder.basicMax[i]) {
                            result.append(String.format(LangID.getStringByID("treasure_range", lang), texts[i], 1, TreasureHolder.basicMax[i]))
                                    .append("\n\n");
                        } else {
                            treasure.basic[i] = v;
                            done[i] = true;
                        }
                    }
                }

                key = "treasure_basicdone";
            }
            case EOC -> {
                String[] values = prepareValues(event, "research", "study", "account", "health", "attack");

                String[] texts = prepareTexts(
                        TreasureHolder.eocText[TreasureHolder.T_RESEARCH],
                        TreasureHolder.eocText[TreasureHolder.T_STUDY],
                        TreasureHolder.eocText[TreasureHolder.T_ACCOUNTANT],
                        TreasureHolder.eocText[TreasureHolder.T_HEALTH],
                        TreasureHolder.eocText[TreasureHolder.T_ATTACK]
                );

                done = new boolean[values.length];

                for (int i = 0; i < values.length; i++) {
                    if (!StaticStore.isNumeric(values[i])) {
                        result.append(String.format(LangID.getStringByID("treasure_notnum", lang), texts[i]))
                                .append("\n\n");
                    } else {
                        int v = StaticStore.safeParseInt(values[i]);

                        if (v < 0 || v > TreasureHolder.eocMax[i]) {
                            result.append(String.format(LangID.getStringByID("treasure_range", lang), texts[i], 0, TreasureHolder.eocMax[i]))
                                    .append("\n\n");
                        } else {
                            treasure.eoc[i] = v;
                            done[i] = true;
                        }
                    }
                }

                key = "treasure_eocdone";
            }
            case ITF -> {
                String[] values = prepareValues(event, "crystal", "black", "red", "float", "angel");

                String[] texts = prepareTexts(
                        TreasureHolder.eocText[TreasureHolder.T_ITF_CRYSTAL],
                        TreasureHolder.eocText[TreasureHolder.T_BLACK],
                        TreasureHolder.eocText[TreasureHolder.T_RED],
                        TreasureHolder.eocText[TreasureHolder.T_FLOAT],
                        TreasureHolder.eocText[TreasureHolder.T_ANGEL]
                );

                done = new boolean[values.length];

                for (int i = 0; i < values.length; i++) {
                    if (!StaticStore.isNumeric(values[i])) {
                        result.append(String.format(LangID.getStringByID("treasure_notnum", lang), texts[i]))
                                .append("\n\n");
                    } else {
                        int v = StaticStore.safeParseInt(values[i]);

                        if (v < 0 || v > TreasureHolder.itfMax[i]) {
                            result.append(String.format(LangID.getStringByID("treasure_range", lang), texts[i], 0, TreasureHolder.itfMax[i]))
                                    .append("\n\n");
                        } else {
                            treasure.itf[i] = v;
                            done[i] = true;
                        }
                    }
                }

                key = "treasure_itfdone";
            }
            case COTC -> {
                String[] values = prepareValues(event, "crystal", "metal", "zombie", "alien", "study");

                String[] texts = prepareTexts(
                        TreasureHolder.eocText[TreasureHolder.T_COTC_CRYSTAL],
                        TreasureHolder.eocText[TreasureHolder.T_METAL],
                        TreasureHolder.eocText[TreasureHolder.T_ZOMBIE],
                        TreasureHolder.eocText[TreasureHolder.T_ALIEN],
                        TreasureHolder.eocText[TreasureHolder.T_STUDY2]
                );

                done = new boolean[values.length];

                for (int i = 0; i < values.length; i++) {
                    if (!StaticStore.isNumeric(values[i])) {
                        result.append(String.format(LangID.getStringByID("treasure_notnum", lang), texts[i]))
                                .append("\n\n");
                    } else {
                        int v = StaticStore.safeParseInt(values[i]);

                        if (v < 0 || v > TreasureHolder.cotcMax[i]) {
                            result.append(String.format(LangID.getStringByID("treasure_range", lang), texts[i], 0, TreasureHolder.cotcMax[i]))
                                    .append("\n\n");
                        } else {
                            treasure.cotc[i] = v;
                            done[i] = true;
                        }
                    }
                }

                key = "treasure_cotcdone";
            }
        }

        editor.run();

        if(result.isEmpty() && key != null) {
            event.reply(LangID.getStringByID(key, lang))
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();
        } else {
            if(somethingDone(done)) {
                result.append(LangID.getStringByID("treasure_other", lang));
            }

            event.reply(result.toString())
                    .mentionRepliedUser(false)
                    .setEphemeral(true)
                    .queue();
        }
    }

    private String[] prepareValues(ModalInteractionEvent event, String... key) {
        String[] result = new String[key.length];

        List<ModalMapping> mapping = event.getValues();

        for(int i = 0; i < key.length; i++) {
            result[i] = getValueFromMap(mapping, key[i]);
        }

        return result;
    }

    private String[] prepareTexts(String... key) {
        String[] result = new String[key.length];

        for(int i = 0; i < key.length; i++) {
            result[i] = LangID.getStringByID(key[i], lang);
        }

        return result;
    }

    private boolean somethingDone(boolean[] data) {
        for(int i = 0; i < data.length; i++) {
            if(data[i])
                return true;
        }

        return false;
    }
}
