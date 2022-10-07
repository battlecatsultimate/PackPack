package mandarin.packpack.supporter.server.holder;

import common.system.files.VFile;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class AssetBrowserHolder extends SearchHolder implements Comparator<VFile> {
    @Nonnull
    private VFile vf;

    private final List<VFile> files = new ArrayList<>();

    public AssetBrowserHolder(@NotNull Message msg, @NotNull Message author, @NotNull String channelID, @Nonnull VFile vf, int lang) {
        super(msg, author, channelID, lang);

        this.vf = vf;

        Collection<VFile> fileList = vf.list();

        if(fileList == null) {
            throw new IllegalStateException("E/AssetBrowserHolder - ./org file list returned null");
        }

        files.addAll(fileList);
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> result = new ArrayList<>();

        if(!vf.getName().equals("org")) {
            if(onText) {
                result.add(LangID.getStringByID("asset_goup", lang));
            } else {
                result.add(EmojiStore.FOLDERUP.getAsMention() + "\\\\" + LangID.getStringByID("asset_goup", lang));
            }
        }

        for(int i = page * PAGE_CHUNK; i < (page + 1) * PAGE_CHUNK; i++) {
            if(i >= files.size()) {
                break;
            }

            VFile vf = files.get(i);

            if(vf.getData() == null) {
                if(!onText) {
                    result.add(EmojiStore.FOLDER.getAsMention() + "\\\\" + vf.getName().replace("_", "＿"));
                } else {
                    result.add(vf.getName().replace("_", "＿"));
                }
            } else {
                String name = "";

                if (!onText) {
                    String[] nameData = vf.getName().split("\\.");

                    RichCustomEmoji emoji;

                    switch (nameData[1]) {
                        case "png":
                            emoji = EmojiStore.PNG;

                            break;
                        case "csv":
                            emoji = EmojiStore.CSV;

                            break;
                        case "tsv":
                            emoji = EmojiStore.TSV;

                            break;
                        case "json":
                            emoji = EmojiStore.JSON;

                            break;
                        case "ini":
                            emoji = EmojiStore.INI;

                            break;
                        case "imgcut":
                            emoji = EmojiStore.IMGCUT;

                            break;
                        case "mamodel":
                            emoji = EmojiStore.MAMODEL;

                            break;
                        case "maanim":
                            emoji = EmojiStore.MAANIM;

                            break;
                        default:
                            emoji = EmojiStore.FILE;

                            break;
                    }

                    name += emoji.getAsMention() + "\\\\";
                }

                result.add(name + vf.getName().replace("_", "＿"));
            }
        }

        return result;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        if(!vf.getName().equals("org")) {
            id--;
        }

        if(id - page * PAGE_CHUNK == -1)
            throw new IllegalStateException("E/AssetBrowserHolder::onSelected - Invalid ID found : -1");

        VFile file = files.get(id);

        if(file.getData() == null)
            throw new IllegalStateException("E/AssetBrowserHolder::onSelected - Folder passed through the filter");

        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("W/AssetBrowserHolder::onSelected - Failed to create folder : " + temp.getAbsolutePath());

            return;
        }

        try {
            String[] name = file.getName().split("\\.");

            if(name.length != 2) {
                StaticStore.logger.uploadLog("W/AssetBrowserHolder::onSelected - Faile name contains multiple dots : " + file.getName());

                return;
            }

            File f = StaticStore.generateTempFile(temp, name[0], name[1], false);

            if(f == null || !f.exists())
                return;

            InputStream stream = file.getData().getStream();
            FileOutputStream fos = new FileOutputStream(f);

            int l;
            byte[] buffer = new byte[65536];

            while((l = stream.read(buffer)) != -1) {
                fos.write(buffer, 0, l);
            }

            fos.close();
            stream.close();

            Command.sendMessageWithFile(ch, LangID.getStringByID("asset_file", lang).replace("_", file.getName()), f, file.getName());

            msg.delete().queue();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AssetBrowserHolder::onSelected - Failed to perform interaction");
        }
    }

    @Override
    public int getDataSize() {
        return files.size();
    }

    @Override
    public void finish(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        if(!vf.getName().equals("org"))
            id--;

        VFile file;

        if(id - page * PAGE_CHUNK == -1) {
            file = vf.getParent();
        } else {
            file = files.get(id);
        }

        if(file.getData() == null) {
            vf = file;

            updateVFile();
            apply(event);
        } else {
            super.finish(event);
        }
    }

    private void updateVFile() {
        files.clear();
        page = 0;

        Collection<VFile> fileList = vf.list();

        if(fileList == null) {
            throw new IllegalStateException("E/AssetBrowserHolder::updateVFile - File list returned null\n\nPath : " + vf.getPath());
        }

        files.addAll(fileList);

        files.sort(this);
    }

    @Override
    protected String getPage() {
        StringBuilder builder = new StringBuilder(LangID.getStringByID("asset_current", lang).replace("_", vf.getPath()))
                .append("\n\n```md\n");

        List<String> data = accumulateListData(true);

        builder.append(LangID.getStringByID("formst_pick", lang));

        for(int i = 0; i < data.size(); i++) {
            if(!vf.getName().equals("org")) {
                if(i == 0) {
                    builder.append(data.get(i)).append("\n");
                } else {
                    builder.append(page * PAGE_CHUNK + i).append(". ").append(data.get(i)).append("\n");
                }
            } else {
                builder.append(page * PAGE_CHUNK + i + 1).append(". ").append(data.get(i)).append("\n");
            }
        }

        if(files.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = files.size() / SearchHolder.PAGE_CHUNK;

            if(files.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            builder.append(LangID.getStringByID("formst_page", lang).replace("_", (page + 1) + "").replace("-", String.valueOf(totalPage))).append("\n");
        }

        builder.append("```");

        return builder.toString();
    }


    @Override
    public int compare(VFile o1, VFile o2) {
        if(o1.getData() == null && o2.getData() == null) {
            return o1.getName().compareTo(o2.getName());
        } else if(o1.getData() == null)
            return -1;
        else
            return 1;
    }
}
