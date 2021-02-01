package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;

interface Command {
    int DEFAULT_ERROR = -1;
    Pauser pause = new Pauser();

    default void execute(MessageCreateEvent event) {
        try {
            doSomething(event);
        } catch (Exception e) {
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    void doSomething(MessageCreateEvent event) throws Exception;

    default void onFail(MessageCreateEvent event, int error) {
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

    default String getMessage(MessageCreateEvent event) {
        return event.getMessage().getContent();
    }
}
