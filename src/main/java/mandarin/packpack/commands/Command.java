package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

@SuppressWarnings("unused")
public interface Command {
    int DEFAULT_ERROR = -1;
    Pauser pause = new Pauser();

    default void execute(MessageEvent event) {
        try {
            new Thread(() -> {
                try {
                    doSomething(event);
                } catch (Exception e) {
                    e.printStackTrace();
                    onFail(event, DEFAULT_ERROR);
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    void doSomething(MessageEvent event) throws Exception;

    default void onFail(MessageEvent event, int error) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createMessage(StaticStore.ERROR_MSG).subscribe();
    }

    default void onSuccess(MessageEvent event) {}

    default void onCancel(MessageEvent event) {}

    default MessageChannel getChannel(MessageEvent event) {
        Message msg = getMessage(event);

        return msg == null ? null : msg.getChannel().block();
    }

    default Message getMessage(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMessage");

            Object obj = m.invoke(event);

            if(obj instanceof Mono) {
                return (Message) ((Mono<?>) obj).block();
            } else if(obj instanceof Message)
                return (Message) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    default String getContent(MessageEvent event) {
        Message msg = getMessage(event);

        return msg == null ? null : msg.getContent();
    }

    @SuppressWarnings("unchecked")
    default Optional<Member> getMember(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMember");

            Object obj = m.invoke(event);

            if(obj instanceof Optional)
                return (Optional<Member>) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    default Mono<Guild> getGuild(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getGuild");

            Object obj = m.invoke(event);

            if(obj instanceof Mono)
                return (Mono<Guild>) obj;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return null;
    }
}
