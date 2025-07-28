package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.FormButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.List;

public class FormStatMessageHolder extends SearchHolder {
    private final ArrayList<Form> form;
    private final ConfigHolder config;

    private final FormStat.FormStatConfig configData;
    private final TreasureHolder t;

    public FormStatMessageHolder(ArrayList<Form> form, Message author, String userID, String channelID, Message message, String keyword, ConfigHolder config, TreasureHolder t, FormStat.FormStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, config.searchLayout, lang);

        this.form = form;
        this.config = config;

        this.configData = configData;
        this.t = t;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= form.size())
                break;

            Form f = form.get(i);

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);

                        String name = StaticStore.safeMultiLangGet(f, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(f.uid.id) + "-" + Data.trio(f.fid) + "`";

                        String name = StaticStore.safeMultiLangGet(f, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(f.uid.id) + "-" + Data.trio(f.fid);
                        }

                        text += " " + name;
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
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        try {
            Form f = form.get(id);

            EntityHandler.showUnitEmb(f, event, hasAuthorMessage() ? getAuthorMessage() : null, config, f.unit.forms.length >= 3, t, configData, lang, true, true, result -> {
                User u = event.getUser();

                StaticStore.removeHolder(u.getId(), FormStatMessageHolder.this);

                StaticStore.putHolder(u.getId(), new FormButtonHolder(form.get(id), hasAuthorMessage() ? getAuthorMessage() : null, u.getId(), channelID, result, config, t, configData, lang));
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
