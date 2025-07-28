package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
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

    public EnemyDPSHolder(ArrayList<Enemy> form, Message author, String userID, String channelID, Message message, String keyword, ConfigHolder.SearchLayout layout, TreasureHolder t, int magnification, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.form = form;
        this.magnification = magnification;
        this.t = t;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page +1); i++) {
            if(i >= form.size())
                break;

            Enemy e = form.get(i);

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(e.id.id);

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(e.id.id) + "`";

                        String name = StaticStore.safeMultiLangGet(e, lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(e.id.id);
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.safeMultiLangGet(e, lang);

                    if (text == null) {
                        text = Data.trio(e.id.id);
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(e.id.id);
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        MessageChannel ch = event.getChannel();

        message.delete().queue();

        try {
            Enemy f = form.get(index);

            EntityHandler.showEnemyDPS(ch, getAuthorMessage(), f, t, magnification, false, lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/EnemyDPSHolder::onSelected - Failed to perform showing enemy embed");
        }
    }

    @Override
    public int getDataSize() {
        return form.size();
    }
}
