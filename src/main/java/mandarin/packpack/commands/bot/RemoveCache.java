package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class RemoveCache extends ConstraintCommand {
    public RemoveCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ",  2);

        if(contents.length < 2) {
            ch.sendMessage("Format `p!rc [Cache Name]`").queue();

            return;
        }

        if(StaticStore.imgur.removeCache(contents[1])) {
            ch.sendMessage("Successfully removed cache : "+contents[1]).queue();
        } else {
            ch.sendMessage("No such cache found : "+contents[1]).queue();
        }
    }
}
