package mandarin.packpack.commands.data;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.EventDataArchiveHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.*;

public class EventDataArchive extends ConstraintCommand {
    private final ConfigHolder config;

    public EventDataArchive(ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        CommonStatic.Lang.Locale locale = getLocale(loader.getContent());
        String fileName = getFileName(loader.getContent());

        String l = switch (locale) {
            case ZH -> "zh";
            case JP -> "jp";
            case KR -> "kr";
            default -> "en";
        };

        File current = new File("./data/event/" + l + "/" + fileName + ".tsv");

        File archive = new File("./data/event/" + l + "/archive/" + fileName);

        if(!archive.exists() || !current.exists()) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventArchive.failed.noArchive", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        File[] fs = archive.listFiles();

        if(fs == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventArchive.failed.upload", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(fs));

        files.addFirst(current);

        files.sort(Comparator.comparingLong(File::lastModified));
        Collections.reverse(files);

        if(files.isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventArchive.failed.noArchive", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        StringBuilder sb = new StringBuilder(LangID.getStringByID("eventArchive.bringing", lang).replace("_LLL_", l.equals("zh") ? "tw" : l).replace("_FFF_", fileName));

        sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

        List<String> data = accumulateData(files);

        for(int i = 0; i < Math.min(ConfigHolder.SearchLayout.COMPACTED.chunkSize, data.size()); i++) {
            sb.append(i+1).append(". ").append(data.get(i)).append("\n");
        }

        if(files.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            int totalPage = (int) Math.ceil(files.size() * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize);

            sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
        }

        sb.append("```");

        registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), files.size(), data, lang).queue(res -> {
            User u = loader.getUser();

            Message msg = loader.getMessage();

            StaticStore.putHolder(u.getId(), new EventDataArchiveHolder(msg, u.getId(), ch.getId(), res, config.searchLayout, files, fileName, lang));
        });
    }

    public CommonStatic.Lang.Locale getLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-jp" -> {
                    return CommonStatic.Lang.Locale.JP;
                }
                case "-tw", "-zh" -> {
                    return CommonStatic.Lang.Locale.ZH;
                }
                case "-kr" -> {
                    return CommonStatic.Lang.Locale.KR;
                }
                case "-en" -> {
                    return CommonStatic.Lang.Locale.EN;
                }
            }
        }

        return CommonStatic.Lang.Locale.EN;
    }

    public String getFileName(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-g", "-gatya", "-gacha" -> {
                    return "gatya";
                }
                case "-i", "-item" -> {
                    return "item";
                }
                case "-s", "-sale" -> {
                    return "sale";
                }
            }
        }

        return "gatya";
    }

    private List<String> accumulateData(List<File> files) {
        List<String> result = new ArrayList<>();

        for(int i = 0; i < Math.min(files.size(), ConfigHolder.SearchLayout.COMPACTED.chunkSize); i++) {
            if (i == 0) {
                result.add(LangID.getStringByID("eventArchive.currentEvent", lang));
            } else {
                result.add(files.get(i).getName().replace(".txt", "").replaceAll(";", ":"));
            }
        }

        return result;
    }
}
