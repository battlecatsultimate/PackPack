package mandarin.packpack.supporter.server.holder.component.search;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class FormSpriteMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;

    private final int mode;

    public FormSpriteMessageHolder(ArrayList<Form> form, Message author, Message msg, String channelID, int mode, int lang) {
        super(author, msg, channelID, lang);

        this.form = form;
        this.mode = mode;

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

        try {
            Form f = form.get(id);

            EntityHandler.getFormSprite(f, ch, getAuthorMessage(), mode, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormSpriteMessageHolder::onSelected - Failed to upload form sprite/icon");
        }

        msg.delete().queue();
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
