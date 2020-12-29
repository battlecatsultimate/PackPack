package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

public class ServerPrefix extends ConstraintCommand {
    public ServerPrefix(ROLE role) {
        super(role);
    }

    @Override
    public void doSomething(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        String[] list = getMessage(event).split(" ");

        if(list.length == 2) {
            if(list[1] == null || list[1].isBlank()) {
                ch.createMessage("Prefix can't be white spaces!");
                return;
            }

            StaticStore.serverPrefix = list[1];

            ch.createMessage("Server prefix set as "+StaticStore.serverPrefix+"!").subscribe();
        } else if(list.length == 1) {
            ch.createMessage("This command requires one more argument : [Prefix]").subscribe();
        } else {
            ch.createMessage("Too many arguments! This command needs only one argument : [Prefix]").subscribe();
        }
    }
}
