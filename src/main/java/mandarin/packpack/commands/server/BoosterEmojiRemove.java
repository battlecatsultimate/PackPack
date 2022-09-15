package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.BoosterData;
import mandarin.packpack.supporter.server.data.BoosterHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class BoosterEmojiRemove extends ConstraintCommand {
    public BoosterEmojiRemove(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        Guild g = getGuild(event);
        MessageChannel ch = getChannel(event);

        if(ch == null || g == null)
            return;

        String id = getID(getContent(event));

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

                    if(data.getEmoji() == null) {
                        createMessageWithNoPings(ch, LangID.getStringByID("booemorem_noemo", lang));
                    } else {
                        String emoji = data.getEmoji();

                        boolean leave = leaveEmoji(getContent(event));

                        if(!leave) {
                            RichCustomEmoji e = g.getEmojiById(emoji);

                            if(e != null)
                                e.delete().queue();
                        }

                        data.removeRole();

                        if(data.getRole() == null) {
                            holder.serverBooster.remove(m.getId());
                        }

                        createMessageWithNoPings(ch, LangID.getStringByID("booemorem_success", lang).replace("_", m.getId()));
                    }
                } else {
                    createMessageWithNoPings(ch, LangID.getStringByID("booemorem_noemo", lang));
                }
            } else {
                createMessageWithNoPings(ch, LangID.getStringByID("booemorem_noemo", lang));
            }
        }
    }


    private boolean leaveEmoji(String message) {
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
