package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.search.MedalMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class Medal extends ConstraintCommand {
    private final ConfigHolder config;

    public Medal(ROLE role, CommonStatic.Lang.Locale lang, ConfigHolder config, IDHolder id) {
        super(role, lang, id, false);

        if (config == null)
            this.config = holder == null ? StaticStore.defaultConfig : holder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length >= 2) {
            String[] realContents = loader.getContent().split(" ", 2);

            ArrayList<Integer> id = EntityFilter.findMedalByName(realContents[1], lang);

            if(id.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("medal.failed.noEnemy", lang).replace("_", getSearchKeyword(realContents[1])));
            } else if(id.size() == 1) {
                EntityHandler.generateMedalEmbed(id.getFirst(), ch, loader.getMessage(), lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("ui.search.severalResult", lang).replace("_", getSearchKeyword(realContents[1])));

                sb.append("```md\n").append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(id);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(id.size() > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                    int totalPage = SearchHolder.getTotalPage(id.size());

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, id.size(), data, lang), res -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new MedalMessageHolder(id, msg, u.getId(), ch.getId(), res, realContents[1], config.searchLayout, lang));
                });
            }
        } else {
            ch.sendMessage(LangID.getStringByID("medal.failed.noParameter", lang)).queue();
        }
    }

    private List<String> accumulateData(List<Integer> id) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
            if(i >= id.size())
                break;

            String medalName = Data.trio(id.get(i)) + " " + StaticStore.MEDNAME.getCont(id.get(i), lang);

            data.add(medalName);
        }

        return data;
    }

    private String getSearchKeyword(String command) {
        if(command.length() > 1500)
            command = command.substring(0, 1500) + "...";

        return command;
    }
}
