package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class AnnounceMessage extends ConstraintCommand {
    public AnnounceMessage(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if(holder == null)
            return;

        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ", 2);

        if(contents.length < 2) {
            if(holder.announceMessage.isBlank()) {
                replyToMessageSafely(ch, LangID.getStringByID("announce_content", lang), loader.getMessage(), a -> a);
            } else {
                holder.announceMessage = "";

                replyToMessageSafely(ch, LangID.getStringByID("announce_removed", lang), loader.getMessage(), a -> a);
            }

            return;
        }

        if(contents[1].length() > 1500) {
            replyToMessageSafely(ch, LangID.getStringByID("announce_toolong", lang), loader.getMessage(), a -> a);

            return;
        }

        holder.announceMessage = contents[1];

        if(contents[1].length() > 1000) {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("announce_success", lang), contents[1].substring(0, 1500) + "..."), loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(ch, String.format(LangID.getStringByID("announce_success", lang), contents[1]), loader.getMessage(), a -> a);
        }
    }
}
