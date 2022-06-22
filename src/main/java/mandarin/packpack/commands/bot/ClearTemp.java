package mandarin.packpack.commands.bot;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.io.File;

public class ClearTemp extends ConstraintCommand {
    public ClearTemp(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        File temp = new File("./temp");

        StaticStore.deleteFile(temp, false);

        createMessage(ch, m -> m.content("Tried to clean temp folder"));
    }
}
