package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageCreateEvent;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.server.IDHolder;

public class ClearCache extends ConstraintCommand {
    public ClearCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {

    }
}
