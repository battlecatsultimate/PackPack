package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;

public class BoosterRoleRemove extends ConstraintCommand {
    public BoosterRoleRemove(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        Guild g = getGuild(event).block();
        MessageChannel ch = getChannel(event);

        if(ch == null || g == null)
            return;

        String id = getID(getContent(event));

        if(id == null) {
            createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_nomem", lang));
            return;
        }

        g.getMemberById(Snowflake.of(id)).subscribe(m -> {
            if(StaticStore.boosterData.containsKey(g.getId().asString())) {
                BoosterHolder holder = StaticStore.boosterData.get(g.getId().asString());

                if(holder.serverBooster.containsKey(m.getId().asString())) {
                    BoosterData data = holder.serverBooster.get(m.getId().asString());

                    if(data.getRole() == null) {
                        createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_norole", lang));
                    } else {
                        String role = data.getRole();

                        boolean leave = leaveRole(getContent(event));

                        if(leave) {
                            m.removeRole(Snowflake.of(role)).subscribe();
                        } else {
                            g.getRoleById(Snowflake.of(role)).subscribe(r -> r.delete().subscribe());
                        }

                        data.removeRole();

                        if(data.getEmoji() == null) {
                            holder.serverBooster.remove(m.getId().asString());
                        }

                        createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_success", lang).replace("_", m.getId().asString()));
                    }
                } else {
                    createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_norole", lang));
                }
            } else {
                createMessageWithNoPings(ch, LangID.getStringByID("boorolerem_nodata", lang));
            }
        });
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

                if(StaticStore.isNumeric(c))
                    return c;
            } else if(StaticStore.isNumeric(c)) {
                return c;
            }
        }

        return null;
    }
}
