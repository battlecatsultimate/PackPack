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

    public ComboMessageHolder(ArrayList<Combo> combo, Message author, String userID, String channelID, Message message, String keyword, ConfigHolder.SearchLayout layout, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.combo = combo;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1) ; i++) {
            if(i >= combo.size())
                break;

            Combo c = combo.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name));

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

            comboName += " | " + DataToString.getComboType(c, lang) + " ";

            if(c.forms.length == 1) {
                comboName += LangID.getStringByID("combo.slot.singular", lang);
            } else {
                comboName += String.format(LangID.getStringByID("combo.slot.plural", lang), c.forms.length);
            }

            data.add(comboName);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        try {
            EntityHandler.showComboEmbed(event, getAuthorMessage(), combo.get(id), lang, true);

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
    public void onExpire() {

    }
}
