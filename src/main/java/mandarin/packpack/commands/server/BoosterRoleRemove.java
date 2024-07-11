package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class BoosterRoleRemove extends ConstraintCommand {
    public BoosterRoleRemove(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        Guild g = loader.getGuild();
        MessageChannel ch = loader.getChannel();

        String id = getID(loader.getContent());

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_nomem", lang));
            return;
        }

        Member m = g.getMemberById(id);

        if(m != null) {
            if(StaticStore.boosterData.containsKey(g.getId())) {
                BoosterHolder holder = StaticStore.boosterData.get(g.getId());

                if(holder.serverBooster.containsKey(m.getId())) {
                    BoosterData data = holder.serverBooster.get(m.getId());

                    if(data.getRole() == null) {
                        createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_norole", lang));
                    } else {
                        String r = data.getRole();

                        Role role = g.getRoleById(r);

                        if(role != null) {
                            boolean leave = leaveRole(loader.getContent());

                            if(leave) {
                                g.removeRoleFromMember(UserSnowflake.fromId(m.getId()), role).queue();
                            } else {
                                role.delete().queue();
                            }

                            data.removeRole();

                            if(data.getEmoji() == null) {
                                holder.serverBooster.remove(m.getId());
                            }

                            createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_success", lang).replace("_", m.getId()));
                        }
                    }
                } else {
                    createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_norole", lang));
                }
            } else {
                createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_nodata", lang));
            }
        }
    }

    private boolean leaveRole(String message) {
        String[] content = message.split(" ");

        for(String c : content) {
            if(c.equals("-l") || c.equals("-leave"))
                return true;
        }

        return false;
    }

    private String getID(String message) {
        String[] content = message.split(" ");

        for(String c : content) {
            if((c.startsWith("<@") && c.endsWith(">"))) {
                c = c.replaceAll("<@!?", "").replace(">", "").strip();
            }

            if(StaticStore.isNumeric(c))
                return c;
        }

        return null;
    }
}
