package mandarin.packpack.commands.bot;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class RegisterLogging extends ConstraintCommand {
    public RegisterLogging(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        StaticStore.loggingChannel = ch.getId().asString();

        createMessageWithNoPings(ch, "Logging channel is registered [<#"+StaticStore.loggingChannel+">]");
    }
}
