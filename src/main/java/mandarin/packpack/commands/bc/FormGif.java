package mandarin.packpack.commands.bc;

import discord4j.core.event.domain.message.MessageCreateEvent;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.server.IDHolder;

public class FormGif extends TimedConstraintCommand {
    public FormGif(ROLE role, int lang, IDHolder id, long time) {
        super(role, lang, id, time);
    }

    @Override
    public void doSomething(MessageCreateEvent event) throws Exception {

    }
}
