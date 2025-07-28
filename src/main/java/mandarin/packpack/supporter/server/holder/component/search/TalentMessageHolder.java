package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class TalentMessageHolder extends SearchHolder {
    private final List<Form> form;

    private final boolean isFrame;

    public TalentMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, List<Form> form, boolean isFrame, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.form = form;
        this.isFrame = isFrame;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + " ";

                        if (StaticStore.safeMultiLangGet(f, lang) != null) {
                            text += StaticStore.safeMultiLangGet(f, lang);
                        }
                    } else {
                        text = "`" + Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + "` ";

                        String formName = StaticStore.safeMultiLangGet(f, lang);

                        if (formName == null || formName.isBlank()) {
                            formName = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                        }

                        text += "**" + formName + "**";
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(f, lang);

                    if (text == null) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        try {
            Form f = form.get(index);

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
