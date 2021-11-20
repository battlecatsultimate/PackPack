package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class UnregisterFixing extends ConstraintCommand {
    public UnregisterFixing(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessage(ch, m -> m.content("Please specify guild ID"));
            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            createMessage(ch, m -> m.content("ID `"+contents[1]+"` isn't numeric"));
            return;
        }

        if(!StaticStore.needFixing.contains(contents[1])) {
            createMessage(ch, m -> m.content("This server (`"+contents[1]+"`) isn't registered as fixing server already"));
            return;
        }

        StaticStore.needFixing.remove(contents[1]);

        createMessage(ch, m -> m.content("Removed `"+contents[1]+"` from fixing server list"));
    }
}
