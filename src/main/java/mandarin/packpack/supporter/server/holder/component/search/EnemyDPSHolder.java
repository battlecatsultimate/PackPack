package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class EnemyDPSHolder extends SearchHolder {
    private final ArrayList<Enemy> form;
    private final int magnification;

    private final TreasureHolder t;

    public EnemyDPSHolder(ArrayList<Enemy> form, Message author, Message msg, String channelID, TreasureHolder t, int magnification, CommonStatic.Lang.Locale lang) {
        super(author, msg, channelID, lang);

        this.form = form;
        this.magnification = magnification;
        this.t = t;
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Enemy e = form.get(i);

            String eName = Data.trio(e.id.id);

            if(MultiLangCont.get(e, lang) != null)
                eName += MultiLangCont.get(e, lang);

            data.add(eName);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        message.delete().queue();

        try {
            Enemy f = form.get(id);

            EntityHandler.showEnemyDPS(ch, getAuthorMessage(), f, t, magnification, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EnemyDPSHolder::onSelected - Failed to perform showing enemy embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
