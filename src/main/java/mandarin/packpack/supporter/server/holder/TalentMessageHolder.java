package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TalentMessageHolder extends SearchHolder {
    private final List<Form> form;

    private final boolean isFrame;

    public TalentMessageHolder(@NotNull Message msg, @NotNull Message author, @NotNull String channelID, List<Form> form, boolean isFrame, int lang) {
        super(msg, author, channelID, lang);

        this.form = form;
        this.isFrame = isFrame;
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
            Form f = form.get(id);

            if(f.unit.forms.length < 3) {
                createMessageWithNoPings(ch, LangID.getStringByID("talentinfo_notf", lang));

                return;
            }

            Form trueForm = f.unit.forms[2];

            if(trueForm.du == null || trueForm.du.getPCoin() == null) {
                createMessageWithNoPings(ch, LangID.getStringByID("talentinfo_notal", lang));

                return;
            }

            EntityHandler.showTalentEmbed(ch, getAuthorMessage(), trueForm, isFrame, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/TalentMessageHolder::onSelected - Failed to perform showing talent embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
