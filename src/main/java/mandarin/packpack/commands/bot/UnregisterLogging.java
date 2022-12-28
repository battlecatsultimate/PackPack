package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class UnregisterLogging extends ConstraintCommand {
    public UnregisterLogging(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        StaticStore.loggingChannel = "";

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        createMessageWithNoPings(ch, "Logging channel unregistered");
    }
}
