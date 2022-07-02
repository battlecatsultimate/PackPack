package mandarin.packpack.commands.server;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.ArrayList;

public class Publish extends ConstraintCommand {
    public Publish(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        JDA client = event.getJDA();

        if(!StaticStore.announcements.containsKey(0)) {
            createMessageWithNoPings(ch, "You have to at least make announcement for English!");
            return;
        }

        for(String id : StaticStore.idHolder.keySet()) {
            if(id == null || id.isBlank())
                continue;

            IDHolder holder = StaticStore.idHolder.get(id);

            if(holder.ANNOUNCE == null)
                continue;

            Guild g = client.getGuildById(id);

            if(g == null)
                continue;

            GuildChannel c = g.getGuildChannelById(holder.ANNOUNCE);

            if(c instanceof NewsChannel) {
                String content = null;

                int[] pref = CommonStatic.Lang.pref[holder.serverLocale];

                for(int p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        content = StaticStore.announcements.get(p);
                        break;
                    }
                }

                if(content != null) {
                    Message m = ((NewsChannel) c)
                            .sendMessage(content)
                            .allowedMentions(new ArrayList<>())
                            .complete();

                    if(m != null && holder.publish)
                        m.crosspost().queue();
                }
            } else if(c instanceof GuildMessageChannel) {
                String content = null;

                int[] pref = CommonStatic.Lang.pref[holder.serverLocale];

                for(int p : pref) {
                    if(StaticStore.announcements.containsKey(p)) {
                        content = StaticStore.announcements.get(p);
                        break;
                    }
                }

                if(content != null) {
                    ((GuildMessageChannel) c).sendMessage(content)
                            .allowedMentions(new ArrayList<>())
                            .queue();
                }
            }
        }

        StaticStore.announcements.clear();

        createMessageWithNoPings(ch, "Successfully announced");
    }
}
