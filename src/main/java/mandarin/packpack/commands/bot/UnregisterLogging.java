package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class UnregisterLogging extends ConstraintCommand {
    public UnregisterLogging(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        StaticStore.loggingChannel = "";

        MessageChannel ch = loader.getChannel();

        createMessageWithNoPings(ch, "Logging channel unregistered");
    }
}
