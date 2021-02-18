package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import mandarin.packpack.supporter.server.IDHolder;

public class Test extends ConstraintCommand {

    public Test(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {

    }


}
