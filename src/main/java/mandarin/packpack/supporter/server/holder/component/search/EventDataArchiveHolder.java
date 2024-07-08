package mandarin.packpack.supporter.server.holder.component.search;

import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EventDataArchiveHolder extends SearchHolder {
    private final List<File> files;
    private final String fileName;

    public EventDataArchiveHolder(@NotNull Message msg, @NotNull Message author, @NotNull String channelID, List<File> files, String fileName, int lang) {
        super(author, msg, channelID, lang);

        this.files = files;
        this.fileName = fileName;

        registerAutoFinish(this, msg, lang, FIVE_MIN);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> result = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < Math.min(files.size(), PAGE_CHUNK * (page + 1)); i++) {
            if (i == 0) {
                result.add(LangID.getStringByID("eventarc_current", lang));
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

        ch.sendMessage(LangID.getStringByID("eventarc_success", lang))
                .setAllowedMentions(new ArrayList<>())
                .addFiles(FileUpload.fromData(f, f.getName().replace(".txt", "").replaceAll(";", "-") + "_" + fileName + ".txt"))
                .queue();

        message.delete().queue();
    }

    @Override
    public int getDataSize() {
        return files.size();
    }
}
