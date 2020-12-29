package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

public class Save extends ConstraintCommand {
    public Save(ROLE role) {
        super(role);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        Message msg = ch.createMessage("Saving data...").block();

        if(msg != null) {
            StaticStore.saveServerInfo();

            msg.edit(e -> e.setContent("Done!")).subscribe();
        } else {
            onFail(event, DEFAULT_ERROR);
        }
    }
}
