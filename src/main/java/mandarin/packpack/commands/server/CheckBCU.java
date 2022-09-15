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

import java.util.List;

public class CheckBCU extends Command {
    private final IDHolder holder;

    public CheckBCU(int lang, IDHolder holder) {
        super(lang);

        this.holder = holder;
    }

    @Override
    public void doSomething(GenericMessageEvent event) {
        Message msg = getMessage(event);

        if(msg == null)
            return;

        MessageChannel ch = msg.getChannel();

        String preID = getPreMemberID(getContent(event));

        if(preID == null && !holder.ID.containsKey("Pre Member")) {
            ch.sendMessage(LangID.getStringByID("chbcu_pre", lang)).queue();

            return;
        } else if(preID == null && holder.ID.containsKey("Pre Member")) {
            preID = holder.ID.get("Pre Member");
        }

        final String finalPre = preID;

        if(StaticStore.checkingBCU) {
            ch.sendMessage(LangID.getStringByID("chbcu_perform", lang)).queue();
        } else {
            StaticStore.checkingBCU = true;

            StringBuilder both = new StringBuilder("BOTH : ");
            StringBuilder none = new StringBuilder("NONE : ");

            Guild g = getGuild(event);

            if(g != null) {
                List<Member> members = g.getMembers();

                for(int i = 0; i < members.size(); i++) {
                    Member m = members.get(i);

                    if(!m.getUser().isBot() && (!holder.ID.containsKey("Muted") || !StaticStore.rolesToString(m.getRoles()).contains(holder.ID.get("Muted")))) {
                        boolean pre = false;
                        boolean mem = false;

                        String role = StaticStore.rolesToString(m.getRoles());

                        if(role.contains(finalPre))
                            pre = true;

                        if(holder.MEMBER != null && role.contains(holder.MEMBER))
                            mem = true;

                        if (!pre && !mem)
                            none.append(m.getNickname()).append(", ");

                        if (pre && mem)
                            both.append(m.getNickname()).append(", ");
                    }
                }

                ch.sendMessage(both.substring(0, both.length()-2)+"\n"+none.substring(0, none.length()-2)).queue();
            }

            StaticStore.checkingBCU = false;
        }
    }

    private String getPreMemberID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-p") || contents[i].equals("-pre")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }
}
