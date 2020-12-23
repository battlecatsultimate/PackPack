package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;

interface Command {
    default void execute(MessageCreateEvent event) {
        try {
            doSomething(event);
        } catch (Exception e) {
            onFail(event);
        }
    }

    void doSomething(MessageCreateEvent event);

    default void onFail(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createMessage(StaticStore.ERROR_MSG).subscribe();
    }

    default void onSuccess(MessageCreateEvent event) {}

    default void onCancel(MessageCreateEvent event) {}

    default MessageChannel getChannel(MessageCreateEvent event) {
        return event.getMessage().getChannel().block();
    }
}
