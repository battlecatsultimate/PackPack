package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EventDataArchiveHolder extends SearchHolder {
    private final List<File> files;
    private final String fileName;

    public EventDataArchiveHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder.SearchLayout layout, List<File> files, String fileName, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, "", layout, lang);

        this.files = files;
        this.fileName = fileName;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        if (textType == TextType.LIST_DESCRIPTION)
            return null;

        List<String> result = new ArrayList<>();

        for(int i = chunk * page; i < Math.min(files.size(), chunk * (page + 1)); i++) {
            if (i == 0) {
                result.add(LangID.getStringByID("eventArchive.currentEvent", lang));
            } else {
                result.add(files.get(i).getName().replace(".txt", "").replaceAll(";", ":"));
            }
        }

        return result;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        File f = files.get(id);

        String uploadFileName;

        if (id == 0) {
            uploadFileName = "current_" + fileName + ".txt";
        } else {
            uploadFileName = f.getName().replace(".txt", "").replaceAll(";", "-") + "_" + fileName + ".txt";
        }

        ch.sendMessage(LangID.getStringByID("eventArchive.success", lang))
                .setAllowedMentions(new ArrayList<>())
                .addFiles(FileUpload.fromData(f, uploadFileName))
                .queue();

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return files.size();
    }
}
