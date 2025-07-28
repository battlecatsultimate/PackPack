package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class MedalMessageHolder extends SearchHolder {
    private final ArrayList<Integer> id;
    
    public MedalMessageHolder(ArrayList<Integer> id, Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.id = id;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1) ; i++) {
            if(i >= id.size())
                break;

            String text = null;

            switch(textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(id.get(i));

                        String name = StaticStore.MEDNAME.getCont(id.get(i), lang);

                        if (name != null && !name.isBlank()) {
                            text += " " + name;
                        }
                    } else {
                        text = "`" + Data.trio(id.get(i)) + "`";

                        String name = StaticStore.MEDNAME.getCont(id.get(i), lang);

                        if (name == null || name.isBlank()) {
                            name = Data.trio(id.get(i));
                        }

                        text += " " + name;
                    }
                }
                case LIST_LABEL -> {
                    text = StaticStore.MEDNAME.getCont(id.get(i), lang);

                    if (text == null) {
                        text = Data.trio(id.get(i));
                    }
                }
                case LIST_DESCRIPTION -> text = Data.trio(id.get(i));
            }

            data.add(text);

            String medalName = Data.trio(id.get(i)) + " " + StaticStore.MEDNAME.getCont(id.get(i), lang);

            data.add(medalName);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int i = parseDataToInt(event);

        message.delete().queue();

        try {
            EntityHandler.showMedalEmbed(id.get(i), ch, getAuthorMessage(), lang);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/MedalMessageHolder::onSelected - Failed to upload medal embed");
        }
    }

    @Override
    public int getDataSize() {
        return id.size();
    }
}
