package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.MedalMessageHolder;
import mandarin.packpack.supporter.server.holder.segment.SearchHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;
import java.util.List;

public class Medal extends ConstraintCommand {
    public Medal(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length >= 2) {
            String[] realContents = getContent(event).split(" ", 2);

            ArrayList<Integer> id = EntityFilter.findMedalByName(realContents[1], lang);

            if(id.isEmpty()) {
                createMessageWithNoPings(ch, LangID.getStringByID("medal_nomed", lang).replace("_", getSearchKeyword(realContents[1])));
            } else if(id.size() == 1) {
                EntityHandler.showMedalEmbed(id.get(0), ch, getMessage(event), lang);
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("formst_several", lang).replace("_", getSearchKeyword(realContents[1])));

                sb.append("```md\n").append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateData(id);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(id.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = id.size() / SearchHolder.PAGE_CHUNK;

                    if(id.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", "1").replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                Message res = getRepliedMessageSafely(ch, sb.toString(), getMessage(event), a -> registerSearchComponents(a, id.size(), data, lang));

                if(res != null) {
                    User u = getUser(event);

                    if(u != null) {
                        Message msg = getMessage(event);

                        if(msg != null)
                            StaticStore.putHolder(u.getId(), new MedalMessageHolder(id, msg, res, lang, ch.getId()));
                    }
                }
            }
        } else {
            ch.sendMessage(LangID.getStringByID("medal_more", lang)).queue();
        }
    }

    private List<String> accumulateData(List<Integer> id) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= id.size())
                break;

            int oldConfig = CommonStatic.getConfig().lang;
            CommonStatic.getConfig().lang = lang;

            String medalName = Data.trio(id.get(i)) + " " + StaticStore.MEDNAME.getCont(id.get(i));

            CommonStatic.getConfig().lang = oldConfig;

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
