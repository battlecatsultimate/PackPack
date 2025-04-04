package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import javax.annotation.Nonnull;

import java.text.DecimalFormat;
import java.util.List;

public class ServerStat extends Command {

    private final IDHolder holder;

    public ServerStat(CommonStatic.Lang.Locale lang, IDHolder holder) {
        super(lang, true);

        this.holder = holder;
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        Message msg = loader.getMessage();

        MessageChannel ch = msg.getChannel();

        DecimalFormat df = new DecimalFormat("#.##");

        StringBuilder result = new StringBuilder();

        long allUsers;

        Guild g = loader.getGuild();

        List<Member> members = g.loadMembers().get();

        long human = 0L;

        for(int i = 0; i < members.size(); i++) {
            if(!members.get(i).getUser().isBot())
                human++;
        }

        result.append(LangID.getStringByID("serverStat.human", lang).replace("_", Long.toString(human)));
        allUsers = human;

        long member = 0L;

        for(int i = 0; i < members.size(); i++) {
            if(holder.member != null && StaticStore.rolesToString(members.get(i).getRoles()).contains(holder.member)) {
                member++;
            } else if(holder.member == null) {
                member++;
            }
        }

        result.append(LangID.getStringByID("serverStat.member", lang).replace("_", String.valueOf(member)).replace("=", df.format(member * 100.0 / allUsers)));

        for(String name : holder.ID.keySet()) {
            String id = holder.ID.get(name);

            if(id == null)
                continue;

            long c = 0L;

            for(int i = 0; i < members.size(); i++) {
                if(!members.get(i).getUser().isBot() && StaticStore.rolesToString(members.get(i).getRoles()).contains(id))
                    c++;
            }

            result.append(LangID.getStringByID("serverStat.role", lang).replace("_MMM_", String.valueOf(c)).replace("=", df.format(c * 100.0 / allUsers)).replace("_NNN_", limitName(name)));
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
