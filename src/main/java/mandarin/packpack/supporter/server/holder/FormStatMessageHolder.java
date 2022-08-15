package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class FormStatMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;
    private final ConfigHolder config;
    private final Message author;

    private final boolean talent;
    private final boolean isFrame;
    private final boolean extra;
    private final boolean compact;
    private final ArrayList<Integer> lv;

    public FormStatMessageHolder(ArrayList<Form> form, Message author, ConfigHolder config, Message msg, String channelID, int param, ArrayList<Integer> lv, int lang) {
        super(msg, channelID, author.getAuthor().getId(), lang);

        this.form = form;
        this.config = config;
        this.author = author;

        this.talent = (param & 2) > 0;
        this.isFrame = (param & 4) == 0 && config.useFrame;
        this.extra = (param & 8) > 0 || config.extra;
        this.compact = (param & 16) > 0 || config.compact;
        this.lv = lv;

        registerAutoFinish(this, msg, author, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String fname = Data.trio(f.uid.id)+"-"+Data.trio(f.fid)+" ";

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            if(MultiLangCont.get(f) != null)
                fname += MultiLangCont.get(f);

            CommonStatic.getConfig().lang = oldConfig;

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
            Message result = EntityHandler.showUnitEmb(form.get(id), ch, config, isFrame, talent, extra, lv, lang, true, compact);

            if(result != null) {
                Member m = event.getMember();

                if(m != null) {
                    StaticStore.removeHolder(m.getId(), FormStatMessageHolder.this);

                    StaticStore.putHolder(m.getId(), new FormButtonHolder(form.get(id), author, result, config, isFrame, talent, extra, compact, lv, lang, channelID));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
