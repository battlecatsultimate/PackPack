package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class AnnounceMessage extends ConstraintCommand {
    public AnnounceMessage(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        if(holder == null)
            return;

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 2);

        if(contents.length < 2) {
            if(holder.announceMessage.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("announce_content", lang), getMessage(event), a -> a);
            } else {
                holder.announceMessage = "";

                replyToMessageSafely(ch, LangID.getStringByID("announce_removed", lang), getMessage(event), a -> a);
            }

            return;
        }

        if(contents[1].length() > 2000) {
            replyToMessageSafely(ch, LangID.getStringByID("announce_toolong", lang), getMessage(event), a -> a);

            return;
        }

        holder.announceMessage = contents[1];

        if(contents[1].length() > 1500) {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("announce_success", lang), contents[1].substring(0, 1500) + "..."), getMessage(event), a -> a);
        } else {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("announce_success", lang), contents[1]), getMessage(event), a -> a);
        }
    }
}
