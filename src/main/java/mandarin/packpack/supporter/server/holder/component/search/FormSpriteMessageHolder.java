package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class FormSpriteMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;

    private final int mode;

    public FormSpriteMessageHolder(ArrayList<Form> form, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, int mode, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.form = form;
        this.mode = mode;
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
        MessageChannel ch = event.getChannel();

        try {
            Form f = form.get(index);

            EntityHandler.getFormSprite(f, ch, getAuthorMessage(), mode, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormSpriteMessageHolder::onSelected - Failed to upload form sprite/icon");
        }

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
