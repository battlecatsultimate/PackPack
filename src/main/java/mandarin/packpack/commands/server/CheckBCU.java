package mandarin.packpack.commands.server;

import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CheckBCU extends Command {
    private final IDHolder holder;

    public CheckBCU(int lang, @Nullable IDHolder holder) {
        super(lang, true);

        this.holder = holder;
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        Message msg = loader.getMessage();

        MessageChannel ch = msg.getChannel();

        String preID = getPreMemberID(loader.getContent());

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

            int bothMember = 0;
            int noneMember = 0;

            Guild g = loader.getGuild();

            List<Member> members = g.loadMembers().get();

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

                    if (!pre && !mem) {
                        none.append(m.getEffectiveName()).append(", ");
                        noneMember++;
                    }

                    if (pre && mem) {
                        both.append(m.getEffectiveName()).append(", ");
                        bothMember++;
                    }
                }
            }

            String message = both.substring(0, both.length()-2)+"\n"+none.substring(0, none.length()-2);

            if(message.length() >= 2000) {
                message = String.format("BOTH : %d\nNONE : %d", bothMember, noneMember);
            }

            ch.sendMessage(message).queue();

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
