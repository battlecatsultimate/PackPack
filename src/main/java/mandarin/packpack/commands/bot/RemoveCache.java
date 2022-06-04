package mandarin.packpack.commands.bot;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class RemoveCache extends ConstraintCommand {
    public RemoveCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ",  2);

        if(contents.length < 2) {
            createMessage(ch, m -> m.content("Format `p!rc [Cache Name]`"));
            return;
        }

        if(StaticStore.imgur.removeCache(contents[1])) {
            createMessage(ch, m -> m.content("Successfully removed cache : "+contents[1]));
        } else {
            createMessage(ch, m -> m.content("No such cache found : "+contents[1]));
        }
    }
}
