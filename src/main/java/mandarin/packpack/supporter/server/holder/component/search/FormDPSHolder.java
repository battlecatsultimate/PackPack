package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FormDPSHolder extends SearchHolder {
    private final ArrayList<Form> form;
    private final ConfigHolder config;

    private final Level lv;

    private final boolean talent;
    private final boolean treasure;

    private final TreasureHolder t;

    public FormDPSHolder(ArrayList<Form> form, Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder config, Level lv, boolean talent, boolean isTreasure, TreasureHolder t, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, config.searchLayout, lang);

        this.form = form;
        this.config = config;

        this.talent = talent;
        this.treasure = isTreasure;
        this.lv = lv;
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

            EntityHandler.showFormDPS(event, hasAuthorMessage() ? getAuthorMessage() : null, f, t, lv, config, talent, treasure, true, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FormDPSHolder::onSelected - Failed to perform showing unit embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
