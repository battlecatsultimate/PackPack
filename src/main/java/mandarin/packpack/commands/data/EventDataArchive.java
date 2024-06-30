package mandarin.packpack.commands.data;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.EventDataArchiveHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

public class EventDataArchive extends ConstraintCommand {
    public EventDataArchive(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        int locale = getLocale(loader.getContent());
        String fileName = getFileName(loader.getContent());

        String l = switch (locale) {
            case LangID.ZH -> "zh";
            case LangID.JP -> "jp";
            case LangID.KR -> "kr";
            default -> "en";
        };

        File current = new File("./data/event/" + l + "/" + fileName + ".tsv");

        File archive = new File("./data/event/" + l + "/archive/" + fileName);

        if(!archive.exists() || !current.exists()) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventarc_noarchive", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        File[] fs = archive.listFiles();

        if(fs == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventarc_fail", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        List<File> files = new ArrayList<>(Arrays.asList(fs));

        files.addFirst(current);

        files.sort(Comparator.comparingLong(File::lastModified));
        Collections.reverse(files);

        if(files.isEmpty()) {
            createMessageWithNoPings(ch, LangID.getStringByID("eventarc_noarchive", lang).replace("_", l.equals("zh") ? "tw" : l));

            return;
        }

        StringBuilder sb = new StringBuilder(LangID.getStringByID("eventarc_bring", lang).replace("_LLL_", l.equals("zh") ? "tw" : l).replace("_FFF_", fileName));

        sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

        List<String> data = accumulateData(files);

        for(int i = 0; i < Math.min(SearchHolder.PAGE_CHUNK, data.size()); i++) {
            sb.append(i+1).append(". ").append(data.get(i)).append("\n");
        }

        if(files.size() > SearchHolder.PAGE_CHUNK) {
            int totalPage = (int) Math.ceil(files.size() * 1.0 / SearchHolder.PAGE_CHUNK);

            sb.append(LangID.getStringByID("formst_page", lang).formatted(1, totalPage)).append("\n");
        }

        sb.append("```");

        registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), files.size(), data, lang).queue(res -> {
            User u = loader.getUser();

            Message msg = loader.getMessage();

            StaticStore.putHolder(u.getId(), new EventDataArchiveHolder(res, msg, ch.getId(), files, fileName, lang));
        });
    }

    public int getLocale(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            switch (contents[i]) {
                case "-jp" -> {
                    return LangID.JP;
                }
                case "-zh" -> {
                    return LangID.ZH;
                }
                case "-kr" -> {
                    return LangID.KR;
                }
                case "-en" -> {
                    return LangID.EN;
                }
            }
        }

        return LangID.EN;
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

        for(int i = 0; i < Math.min(files.size(), SearchHolder.PAGE_CHUNK); i++) {
            if (i == 0) {
                result.add(LangID.getStringByID("eventarc_current", lang));
            } else {
                result.add(files.get(i).getName().replace(".txt", "").replaceAll(";", ":"));
            }
        }

        return result;
    }
}
