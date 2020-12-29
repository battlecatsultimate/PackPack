package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

public class Prefix extends ConstraintCommand {
    private static final int ERR_CANT_FIND_MEMBER = 0;

    public Prefix(ROLE role) {
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

            event.getMember().ifPresentOrElse(m -> {
                StaticStore.prefix.put(m.getId().asString(), list[1]);

                ch.createMessage("Prefix set as "+list[1]+"!").subscribe();
            }, () -> onFail(event, ERR_CANT_FIND_MEMBER));
        } else if(list.length == 1) {
            ch.createMessage("This command requires one more argument : [Prefix]").subscribe();
        } else {
            ch.createMessage("Too many arguments! This command needs only one argument : [Prefix]").subscribe();
        }
    }

    @Override
    public void onFail(MessageCreateEvent event, int error) {
        MessageChannel ch = getChannel(event);

        switch (error) {
            case DEFAULT_ERROR:
                ch.createMessage("`INTERNAL_ERROR`").subscribe();
                break;
            case ERR_CANT_FIND_MEMBER:
                ch.createMessage("Couldn't get member info").subscribe();
                break;
        }
    }
}
