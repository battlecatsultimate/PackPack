package mandarin.packpack.supporter.server.holder.component.search;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ComboMessageHolder extends SearchHolder {
    private final ArrayList<Combo> combo;
    private final Message fMsg;

    public ComboMessageHolder(ArrayList<Combo> combo, Message author, Message msg, Message fMsg, String channelID, int lang) {
        super(author, msg, channelID, lang);

        this.combo = combo;
        this.fMsg = fMsg;

        registerAutoFinish(this, msg, lang, TimeUnit.MINUTES.toMillis(5), () -> {
            if (expired)
                return;

            expired = true;

            if(fMsg != null)
                fMsg.delete().queue();
        });
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1) ; i++) {
            if(i >= combo.size())
                break;

            Combo c = combo.get(i);

            String comboName = Data.trio(Integer.parseInt(c.name));

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += " " + MultiLangCont.getStatic().COMNAME.getCont(c, lang);

            comboName += " | " + DataToString.getComboType(c, lang) + " ";

            if(c.forms.length == 1) {
                comboName += LangID.getStringByID("combo_slot", lang);
            } else {
                comboName += String.format(LangID.getStringByID("combo_slots", lang), c.forms.length);
            }

            data.add(comboName);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        message.delete().queue();

        if(fMsg != null)
            fMsg.delete().queue();

        try {
            EntityHandler.showComboEmbed(ch, getAuthorMessage(), combo.get(id), lang);

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
    public void onExpire(String id) {
        super.onExpire(id);

        if(fMsg != null)
            fMsg.delete().queue();
    }
}
