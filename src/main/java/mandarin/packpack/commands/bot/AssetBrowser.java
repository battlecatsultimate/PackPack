package mandarin.packpack.commands.bot;

import common.system.files.VFile;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.AssetBrowserHolder;
import mandarin.packpack.supporter.server.holder.component.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AssetBrowser extends ConstraintCommand {
    public AssetBrowser(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EXT_EMOJI);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        VFile vf = VFile.get("./org");

        if(vf == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("asset_no", lang));

            return;
        }

        List<String> data = accumulateData(vf, false);

        StringBuilder builder = new StringBuilder(LangID.getStringByID("asset_current", lang).replace("_", "./org"))
                .append("\n\n```md\n");

        builder.append(LangID.getStringByID("formst_pick", lang));

        for(int i = 0; i < data.size(); i++) {
            builder.append(i + 1).append(". ").append(data.get(i)).append("\n");
        }

        if(data.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = data.size() / SearchHolder.PAGE_CHUNK;

            if(data.size() % SearchHolder.PAGE_CHUNK != 0)
                totalPage++;

            builder.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
        }

        builder.append("```");

        Message res = registerSearchComponents(ch.sendMessage(builder.toString()).setAllowedMentions(new ArrayList<>()), data.size(), accumulateData(vf, true), lang).complete();

        User u = getUser(event);

        if(u != null) {
            StaticStore.putHolder(u.getId(), new AssetBrowserHolder(getMessage(event), res, ch.getId(), vf, lang));
        }
    }

    public List<String> accumulateData(VFile file, boolean onData) {
        List<String> result = new ArrayList<>();

        Collection<VFile> fileList = file.list();

        if(fileList == null)
            return result;

        List<VFile> files = new ArrayList<>(fileList);

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
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
