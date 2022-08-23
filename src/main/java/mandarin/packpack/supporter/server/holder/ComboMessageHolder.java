package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Combo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
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
        super(msg, author, channelID, lang);

        this.combo = combo;
        this.fMsg = fMsg;

        registerAutoFinish(this, msg, author, lang, TimeUnit.MINUTES.toMillis(5), () -> {
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

            String comboName = Data.trio(Integer.parseInt(c.name)) + " ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.getStatic().COMNAME.getCont(c) != null)
                comboName += MultiLangCont.getStatic().COMNAME.getCont(c) + " | " + DataToString.getComboType(c, lang);

            CommonStatic.getConfig().lang = oldConfig;

            data.add(comboName);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        msg.delete().queue();

        if(fMsg != null)
            fMsg.delete().queue();

        try {
            EntityHandler.showComboEmbed(ch, combo.get(id), lang);

            Member m = event.getMember();

            if(m != null) {
                if(StaticStore.timeLimit.containsKey(m.getId())) {
                    StaticStore.timeLimit.get(m.getId()).put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());
                } else {
                    Map<String, Long> memberLimit = new HashMap<>();

                    memberLimit.put(StaticStore.COMMAND_COMBO_ID, System.currentTimeMillis());

                    StaticStore.timeLimit.put(m.getId(), memberLimit);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return combo.size();
    }

    @Override
    public void expire(String id) {
        super.expire(id);

        if(fMsg != null)
            fMsg.delete().queue();
    }
}
