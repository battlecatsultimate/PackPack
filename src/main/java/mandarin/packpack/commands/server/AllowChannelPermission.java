package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AllowChannelPermission extends ConstraintCommand {
    public AllowChannelPermission(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();
        Guild g = loader.getGuild();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 3) {
            createMessageWithNoPings(ch, LangID.getStringByID("comban_noid", lang));

            return;
        }

        String id = contents[1].replaceAll("(<@(!)?|>)", "");

        if(!StaticStore.isNumeric(id)) {
            createMessageWithNoPings(ch, LangID.getStringByID("comban_nonumber", lang));

            return;
        }

        g.retrieveMemberById(id).queue(m -> {
            if(m == null) {
                createMessageWithNoPings(ch, LangID.getStringByID("comban_nomember", lang));

                return;
            }

            StringBuilder result = new StringBuilder(LangID.getStringByID("idset_result", lang));

            boolean memberDone = false;

            List<String> exceptions = holder.channelException.getOrDefault(m.getId(), new ArrayList<>());

            for(int i = 1; i < contents.length; i++) {
                if(contents[i].equals("-me") && !memberDone) {
                    if(holder.MEMBER != null) {
                        exceptions.remove(holder.MEMBER);

                        result.append(contents[i]).append(" : ");
                        result.append(LangID.getStringByID("allowchp_mem", lang)).append("\n");
                    } else {
                        result.append(contents[i]).append(" : ");
                        result.append(LangID.getStringByID("ignorechp_nomem", lang)).append("\n");
                    }

                    memberDone = true;
                } else if((contents[i].equals("-c") || contents[i].equals("-custom")) && i < contents.length - 1 && contents[i + 1].startsWith("\"")) {
                    Object[] set = getName(contents, i + 1);

                    if(set == null) {
                        result.append(contents[i]).append(" : ");
                        result.append(LangID.getStringByID("idset_opened", lang));
                        continue;
                    }

                    String name = (String) set[0];

                    if(!holder.ID.containsKey(name)) {
                        result.append(contents[i]).append(" ").append(limitName(name)).append(" : ");
                        result.append(LangID.getStringByID("channelpermission_noname", lang)).append("\n");
                        continue;
                    }

                    String customId = holder.ID.get(name);

                    exceptions.remove(customId);

                    result.append(contents[i]).append(" ").append(limitName(name)).append(" : ");
                    result.append(LangID.getStringByID("allowchp_cus", lang).replace("_", limitName(name))).append("\n");

                    i = (int) set[1];
                }
            }

            if(exceptions.isEmpty())
                holder.channelException.remove(m.getId());
            else
                holder.channelException.put(m.getId(), exceptions);

            result.append("\n").append(LangID.getStringByID("idset_result", lang));

            StaticStore.saveServerInfo();

            ch.sendMessage(result.toString()).queue();
        }, e -> createMessageWithNoPings(ch, LangID.getStringByID("comban_nomember", lang)));
    }

    private Object[] getName(String[] contents, int start) {
        StringBuilder res = new StringBuilder();

        res.append(contents[start]);

        if(contents[start].endsWith("\""))
            return new Object[] {res.substring(1, res.length() - 1), start + 1};
        else
            res.append(" ");

        int last;
        boolean ended = false;

        for(last = start + 1; last < contents.length; last++) {
            res.append(contents[last]);

            if(contents[last].endsWith("\"")) {
                ended = true;
                break;
            } else {
                res.append(" ");
            }
        }


        if(!ended)
            return null;

        return new Object[] {res.substring(1, res.length() - 1), last + 1};
    }

    private String limitName(String name) {
        if(name.length() > 20) {
            return name.substring(0, 17) + "...";
        }

        return name;
    }
}
