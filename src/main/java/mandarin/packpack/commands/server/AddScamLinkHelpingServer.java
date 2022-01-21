package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class AddScamLinkHelpingServer extends ConstraintCommand {
    public AddScamLinkHelpingServer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessage(ch, m -> m.content("Usage : p!ashs [Server ID]"));
            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            createMessage(ch, m -> m.content("Server ID must be numeric!"));
            return;
        }


        if(StaticStore.scamLink.servers.contains(contents[1])) {
            createMessage(ch, m -> m.content("This server is already registered as helping server"));
            return;
        }

        StaticStore.scamLink.servers.add(contents[1]);
        createMessage(ch, m -> m.content("Added server "+contents[1]));
    }
}
