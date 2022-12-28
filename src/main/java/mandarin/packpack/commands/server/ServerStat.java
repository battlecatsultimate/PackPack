package mandarin.packpack.commands.server;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.text.DecimalFormat;
import java.util.List;

public class ServerStat extends Command {

    private final IDHolder holder;

    public ServerStat(int lang, IDHolder holder) {
        super(lang, true);

        this.holder = holder;
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        Message msg = getMessage(event);

        if(msg == null)
            return;

        MessageChannel ch = msg.getChannel();

        DecimalFormat df = new DecimalFormat("#.##");

        StringBuilder result = new StringBuilder();

        long allUsers;

        Guild g = getGuild(event);

        if(g != null) {
            List<Member> members = g.loadMembers().get();

            long human = 0L;

            for(int i = 0; i < members.size(); i++) {
                if(!members.get(i).getUser().isBot())
                    human++;
            }

            result.append(LangID.getStringByID("bcustat_human", lang).replace("_", Long.toString(human)));
            allUsers = human;

            long member = 0L;

            for(int i = 0; i < members.size(); i++) {
                if(holder.MEMBER != null && StaticStore.rolesToString(members.get(i).getRoles()).contains(holder.MEMBER))
                    member++;
            }

            result.append(LangID.getStringByID("bcustat_mem", lang).replace("_", String.valueOf(member)).replace("=", df.format(member * 100.0 / allUsers)));

            for(String name : holder.ID.keySet()) {
                String id = holder.ID.get(name);

                if(id == null)
                    continue;

                long c = 0L;

                for(int i = 0; i < members.size(); i++) {
                    if(!members.get(i).getUser().isBot() && StaticStore.rolesToString(members.get(i).getRoles()).contains(id))
                        c++;
                }

                result.append(LangID.getStringByID("bcustat_role", lang).replace("_MMM_", String.valueOf(c)).replace("=", df.format(c * 100.0 / allUsers)).replace("_NNN_", limitName(name)));
            }
        }

        ch.sendMessage(result.toString()).queue();
    }

    private String limitName(String name) {
        if(name.length() > 20) {
            return name.substring(0, 17) + "...";
        }

        return name;
    }
}
