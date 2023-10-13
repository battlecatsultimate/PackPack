package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.ArrayList;
import java.util.List;

public class FixRole extends ConstraintCommand {
    public FixRole(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        Guild g = loader.getGuild();

        if(!StaticStore.needFixing.contains(g.getId())) {
            createMessageWithNoPings(ch, LangID.getStringByID("fixrole_nofixing", lang).replace("_", StaticStore.MANDARIN_SMELL));
            return;
        }

        if(holder.MEMBER == null) {
            ch.sendMessage(LangID.getStringByID("fixrole_nomem", lang)).queue();

            return;
        }

        String preID = getPreMemberID(loader.getContent());

        if(preID == null && holder.ID.containsKey("Pre Member")) {
            preID = holder.ID.get("Pre Member");
        } else if(preID == null && !holder.ID.containsKey("Pre Member")) {
            ch.sendMessage(LangID.getStringByID("chbcu_pre", lang)).queue();

            return;
        }

        if(preID == null)
            return;

        final String finalPre = preID;
        String ignore = getIgnore(loader.getContent());

        String content;

        if(ignore == null) {
            content = LangID.getStringByID("fixrole_confirm", lang).replace("_PPP_", finalPre).replace("_MMM_", holder.MEMBER);
        } else {
            content = LangID.getStringByID("fixrole_confirmig", lang).replace("_PPP_", finalPre).replace("_MMM_", holder.MEMBER).replace("_III_", ignore);
        }

        registerConfirmButtons(ch.sendMessage(content).setAllowedMentions(new ArrayList<>()), lang).queue(msg -> {
            if(msg == null)
                return;

            Member me = loader.getMember();

            List<Member> members = g.loadMembers().get();

            StaticStore.putHolder(me.getId(), new ConfirmButtonHolder(loader.getMessage(), msg, ch.getId(), () -> {
                Role role = g.getRoleById(finalPre);

                if(role == null)
                    return;

                long fixed = 0L;

                for(Member m : members) {
                    String roles = StaticStore.rolesToString(m.getRoles());

                    if(!roles.contains(finalPre) && !roles.contains(holder.MEMBER)) {
                        g.addRoleToMember(UserSnowflake.fromId(m.getId()), role).queue();
                        fixed++;
                    }
                }

                if(fixed == 0) {
                    ch.sendMessage(LangID.getStringByID("fixrole_noneed", lang)).queue();
                } else {
                    ch.sendMessage(LangID.getStringByID("fixrole_fixed", lang).replace("_", String.valueOf(fixed))).queue();
                }
            }, lang));
        });
    }

    private String getPreMemberID(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-p") || contents[i].equals("-pre")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }

    private String getIgnore(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-i") || contents[i].equals("-ignore")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1]))
                return contents[i + 1];
        }

        return null;
    }
}
