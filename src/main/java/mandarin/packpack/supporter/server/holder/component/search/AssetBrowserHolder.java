package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.system.files.VFile;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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

    public AssetBrowserHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder.SearchLayout layout, @Nonnull VFile vf, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, "",  layout, lang);

        this.vf = vf;

        Collection<VFile> fileList = vf.list();

        if(fileList == null) {
            throw new IllegalStateException("E/AssetBrowserHolder - ./org file list returned null");
        }

        files.addAll(fileList);
    }

    @Override
    public String getSearchSummary() {
        return LangID.getStringByID("assetBrowser.currentPath", lang).formatted(vf.getPath());
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> result = new ArrayList<>();

        if(!vf.getName().equals("org")) {
            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        result.add(LangID.getStringByID("assetBrowser.parentFolder", lang));
                    } else {
                        result.add(EmojiStore.FOLDERUP.getFormatted() + " " + LangID.getStringByID("assetBrowser.parentFolder", lang));
                    }
                }
                case LIST_LABEL -> result.add(EmojiStore.FOLDERUP.getFormatted() + "\\\\" + LangID.getStringByID("assetBrowser.parentFolder", lang));
                case LIST_DESCRIPTION -> result.add(vf.getPath());
            }
        }

        for(int i = page * chunk; i < Math.min(files.size(), (page + 1) * chunk); i++) {
            VFile vf = files.get(i);

            if(vf.getData() == null) {
                switch (textType) {
                    case TEXT -> {
                        if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                            result.add(vf.getName());
                        } else {
                            result.add(EmojiStore.FOLDER.getFormatted() + "    " + vf.getName().replace("_", "\\_"));
                        }
                    }
                    case LIST_LABEL -> result.add(EmojiStore.FOLDER.getFormatted() + "\\\\" + vf.getName());
                    case LIST_DESCRIPTION -> result.add(vf.getPath());
                }
            } else {
                String[] nameData = vf.getName().split("\\.");

                Emoji emoji = switch (nameData[1]) {
                    case "png" -> EmojiStore.PNG;
                    case "csv" -> EmojiStore.CSV;
                    case "tsv" -> EmojiStore.TSV;
                    case "json" -> EmojiStore.JSON;
                    case "ini" -> EmojiStore.INI;
                    case "imgcut" -> EmojiStore.IMGCUT;
                    case "mamodel" -> EmojiStore.MAMODEL;
                    case "maanim" -> EmojiStore.MAANIM;
                    default -> EmojiStore.FILE;
                };

                switch (textType) {
                    case TEXT -> {
                        if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                            result.add(vf.getName());
                        } else {
                            result.add(emoji.getFormatted() + "    " + vf.getName().replace("_", "\\_"));
                        }
                    }
                    case LIST_LABEL -> result.add(emoji.getFormatted() + "\\\\" + vf.getName());
                    case LIST_DESCRIPTION -> result.add(vf.getPath());
                }
            }
        }

        return result;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        if(index - page * chunk == -1)
            throw new IllegalStateException("E/AssetBrowserHolder::onSelected - Invalid ID found : -1");

        VFile file = files.get(index);

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

            List<ContainerChildComponent> components = new ArrayList<>();

            components.add(TextDisplay.of(LangID.getStringByID("assetBrowser.uploaded", lang).formatted(file.getName())));

            components.add(Separator.create(true, Separator.Spacing.LARGE));

            if (file.getName().endsWith("png")) {
                components.add(MediaGallery.of(MediaGalleryItem.fromFile(FileUpload.fromData(f, file.getName()))));
            } else {
                components.add(FileDisplay.fromFile(FileUpload.fromData(f, file.getName())));
            }

            event.deferEdit()
                    .setComponents(Container.of(components)).useComponentsV2()
                    .mentionRepliedUser(false)
                    .queue(unused -> StaticStore.deleteFile(f, true), e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/AssetBrowserHolder::onSelected - Failed to send asset file");

                        StaticStore.deleteFile(f, true);
                    });
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/AssetBrowserHolder::onSelected - Failed to perform interaction");
        }
    }

    @Override
    public int getDataSize() {
        return files.size();
    }

    @Override
    public void finish(GenericComponentInteractionCreateEvent event, int index) {
        if(!vf.getName().equals("org"))
            index--;

        VFile file;

        if(index - page * chunk == -1) {
            file = vf.getParent();
        } else {
            file = files.get(index);
        }

        if(file.getData() == null) {
            vf = file;

            updateVFile();
            apply(event);
        } else {
            super.finish(event, index);
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
    public int compare(VFile o1, VFile o2) {
        if(o1.getData() != null && o2.getData() != null) {
            return o1.getName().compareTo(o2.getName());
        } else if(o1.getData() == null)
            return -1;
        else if(o2.getData() == null)
            return 1;
        else
            return 0;
    }
}
