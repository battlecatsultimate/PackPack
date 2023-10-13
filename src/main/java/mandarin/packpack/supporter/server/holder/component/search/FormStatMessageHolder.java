package mandarin.packpack.supporter.server.holder.component.search;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.FormButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class FormStatMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;
    private final ConfigHolder config;

    private final boolean talent;
    private final boolean isFrame;
    private final boolean extra;
    private final boolean compact;
    private final boolean isTrueForm;
    private final Level lv;
    private final boolean treasure;
    private final TreasureHolder t;

    public FormStatMessageHolder(ArrayList<Form> form, Message author, ConfigHolder config, IDHolder holder, Message msg, String channelID, int param, Level lv, TreasureHolder t, int lang) {
        super(author, msg, channelID, lang);

        this.form = form;
        this.config = config;

        this.talent = (param & 2) > 0 || lv.getTalents().length > 0;
        this.isFrame = (param & 4) == 0 && config.useFrame;
        this.extra = (param & 8) > 0 || config.extra;
        this.compact = (param & 16) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        this.isTrueForm = (param & 32) > 0;
        this.treasure = (param & 64) > 0 || config.treasure;
        this.lv = lv;
        this.t = t;

        registerAutoFinish(this, msg, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        msg.delete().queue();

        try {
            Form f = form.get(id);

            EntityHandler.showUnitEmb(f, ch, getAuthorMessage(), config, isFrame, talent, extra, isTrueForm, f.fid == 2, lv, treasure, t, lang, true, compact, result -> {
                User u = event.getUser();

                StaticStore.removeHolder(u.getId(), FormStatMessageHolder.this);

                StaticStore.putHolder(u.getId(), new FormButtonHolder(form.get(id), getAuthorMessage(), result, config, isFrame, talent, extra, compact, treasure, t, lv, lang, channelID));
            });
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormStatMessageHolder::onSelected - Failed to perform showing unit embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
