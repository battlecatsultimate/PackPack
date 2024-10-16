package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TalentMessageHolder extends SearchHolder {
    private final List<Form> form;

    private final boolean isFrame;

    public TalentMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, List<Form> form, boolean isFrame, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

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

            if(MultiLangCont.get(f, lang) != null)
                fname += MultiLangCont.get(f, lang);

            data.add(fname);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            if(f.unit.forms.length < 3) {
                event.deferEdit()
                        .setContent(LangID.getStringByID("talentInfo.failed.noTrueForm", lang))
                        .setComponents()
                        .setEmbeds()
                        .setFiles()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                return;
            }

            Form trueForm = f.unit.forms[2];

            if(trueForm.du == null || trueForm.du.getPCoin() == null) {
                event.deferEdit()
                        .setContent(LangID.getStringByID("talentInfo.failed.noTalent", lang))
                        .setComponents()
                        .setEmbeds()
                        .setFiles()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();

                return;
            }

            EntityHandler.showTalentEmbed(event, hasAuthorMessage() ? getAuthorMessage() : null, trueForm, isFrame, true, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/TalentMessageHolder::onSelected - Failed to perform showing talent embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
