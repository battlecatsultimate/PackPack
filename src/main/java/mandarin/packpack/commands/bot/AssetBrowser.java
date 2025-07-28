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
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
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
            createMessageWithNoPings(ch, LangID.getStringByID("assetBrowser.failed.noAsset", lang));

            return;
        }

        List<String> data = accumulateData(vf, false);

        StringBuilder builder = new StringBuilder(LangID.getStringByID("assetBrowser.currentPath", lang).replace("_", "./org"))
                .append("\n\n```md\n");

        builder.append(LangID.getStringByID("ui.search.selectData", lang));

        for(int i = 0; i < data.size(); i++) {
            builder.append(i + 1).append(". ").append(data.get(i)).append("\n");
        }

        if(data.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = data.size() / ConfigHolder.SearchLayout.COMPACTED.chunkSize;

            if(data.size() % ConfigHolder.SearchLayout.COMPACTED.chunkSize != 0)
                totalPage++;

            builder.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
        }

        builder.append("```");

        registerSearchComponents(ch.sendMessage(builder.toString()).setAllowedMentions(new ArrayList<>()), data.size(), accumulateData(vf, true), lang).queue(res -> {
            User u = loader.getUser();

            StaticStore.putHolder(u.getId(), new AssetBrowserHolder(loader.getMessage(), u.getId(), ch.getId(), res, config.searchLayout, vf, lang));
        });
    }

    public List<String> accumulateData(VFile file, boolean onData) {
        List<String> result = new ArrayList<>();

        Collection<VFile> fileList = file.list();

        if(fileList == null)
            return result;

        List<VFile> files = new ArrayList<>(fileList);

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
            if(i >= files.size())
                break;

            VFile vf = files.get(i);

            if(vf.getData() == null) {
                if(onData) {
                    result.add(EmojiStore.FOLDER.getFormatted() + "\\\\" + vf.getName().replace("_", "＿"));
                } else {
                    result.add(vf.getName().replace("_", "＿"));
                }
            } else {
                String name = "";

                if(onData) {
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

                    name += emoji.getFormatted() + "\\\\";
                }

                result.add(name + vf.getName().replace("_", "＿"));
            }
        }

        return result;
    }
}
