package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComboMessageHolder extends SearchHolder {
    private final ArrayList<Combo> combo;

    private final String unitName;

    public ComboMessageHolder(ArrayList<Combo> combo, Message author, String userID, String channelID, Message message, String unitName, String keyword, ConfigHolder.SearchLayout layout, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.combo = combo;
        this.unitName = unitName;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < Math.min(combo.size(), chunk * (page + 1)); i++) {
            Combo c = combo.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(StaticStore.safeParseInt(c.name));

                        if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                            text += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

                        text += " | " + DataToString.getComboType(c, lang) + " ";

                        if(c.forms.length == 1) {
                            text += LangID.getStringByID("combo.slot.singular", lang);
                        } else {
                            text += String.format(LangID.getStringByID("combo.slot.plural", lang), c.forms.length);
                        }
                    } else {
                        text = "`" + Data.trio(StaticStore.safeParseInt(c.name)) + "`";

                        String comboName = StaticStore.safeMultiLangGet(c, lang);

                        if (comboName == null || comboName.isBlank())
                            comboName = Data.trio(StaticStore.safeParseInt(c.name));

                        text += " **" + comboName;

                        String slots;

                        if(c.forms.length == 1) {
                            slots = LangID.getStringByID("combo.slot.singular", lang);
                        } else {
                            slots = LangID.getStringByID("combo.slot.plural", lang).formatted(c.forms.length);
                        }

                        text += " " + slots + "**\n-# " + DataToString.getComboType(c, lang);
                    }
                }
                case LIST_LABEL -> {
                    text = Data.trio(StaticStore.safeParseInt(c.name));

                    String comboName = StaticStore.safeMultiLangGet(c, lang);

                    if (comboName != null && !comboName.isBlank())
                        text += " " + comboName;

                    String slots;

                    if(c.forms.length == 1) {
                        slots = LangID.getStringByID("combo.slot.singular", lang);
                    } else {
                        slots = LangID.getStringByID("combo.slot.plural", lang).formatted(c.forms.length);
                    }

                    text += " " + slots;
                }
                case LIST_DESCRIPTION -> text = DataToString.getComboType(c, lang);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        try {
            EntityHandler.showComboEmbed(event, getAuthorMessage(), combo.get(index), lang, true);

            User u = event.getUser();

            if(StaticStore.timeLimit.containsKey(u.getId())) {
                StaticStore.timeLimit.get(u.getId()).put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());
            } else {
                Map<String, Long> memberLimit = new HashMap<>();

                memberLimit.put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());

                StaticStore.timeLimit.put(u.getId(), memberLimit);
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/ComboMessageHolder::onSelected - Failed to upload combo embed");
        }
    }

    @Override
    public int getDataSize() {
        return combo.size();
    }

    @Override
    public String getSearchSummary() {
        if (unitName == null || unitName.isBlank()) {
            return LangID.getStringByID("combo.search.comboOnly", lang).formatted(keyword, getDataSize());
        } else if (keyword == null || keyword.isBlank()) {
            return LangID.getStringByID("combo.search.comboOnly", lang).formatted(unitName, getDataSize());
        } else {
            return LangID.getStringByID("combo.search.withUnit", lang).formatted(keyword, unitName, getDataSize());
        }
    }

    @Override
    public void onExpire() {

    }
}
