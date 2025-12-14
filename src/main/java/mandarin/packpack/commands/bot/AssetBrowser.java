package mandarin.packpack.commands.bot;

import common.CommonStatic;
import common.system.files.VFile;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.AssetBrowserHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class AssetBrowser extends ConstraintCommand {
    private final ConfigHolder config;

    public AssetBrowser(ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EXT_EMOJI);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        VFile vf = VFile.get("./org");

        if(vf == null) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("assetBrowser.failed.noAsset", lang));

            return;
        }

        List<VFile> files = new ArrayList<>(vf.list());

        System.out.println(String.join(", ", files.stream().map(VFile::getPath).toArray(String[]::new)));

        replyToMessageSafely(ch, loader.getMessage(), msg ->
            StaticStore.putHolder(loader.getUser().getId(), new AssetBrowserHolder(loader.getMessage(), loader.getUser().getId(), ch.getId(), msg, config.searchLayout, vf, lang))
        , getSearchComponents(files.size(), LangID.getStringByID("assetBrowser.currentPath", lang).formatted("./org"), files, this::accumulateFileName, config.searchLayout, lang));
    }

    public List<String> accumulateFileName(List<VFile> files, SearchHolder.TextType textType) {
        List<String> result = new ArrayList<>();

        for(int i = 0; i < Math.min(files.size(), config.searchLayout.chunkSize); i++) {
            VFile vf = files.get(i);

            if(vf.getData() == null) {
                switch (textType) {
                    case TEXT -> {
                        if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
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
                        if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
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
}
