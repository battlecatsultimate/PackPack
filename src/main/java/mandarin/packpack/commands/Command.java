package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.SpamPrevent;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public abstract class Command {
    public final int DEFAULT_ERROR = -1;
    public Pauser pause = new Pauser();
    public final int lang;

    public Command(int lang) {
        this.lang = lang;
    }

    public void execute(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        AtomicReference<Boolean> prevented = new AtomicReference<>(false);

        getMember(event).ifPresent(m -> {
            SpamPrevent spam;

            if(StaticStore.spamData.containsKey(m.getId().asString())) {
                spam = StaticStore.spamData.get(m.getId().asString());

                prevented.set(spam.isPrevented(ch, lang, m.getId().asString()));
            } else {
                spam = new SpamPrevent();

                StaticStore.spamData.put(m.getId().asString(), spam);
            }
        });

        if (prevented.get())
            return;

        StaticStore.executed++;

        try {
            new Thread(() -> {
                try {
                    doSomething(event);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
                    e.printStackTrace();
                    onFail(event, DEFAULT_ERROR);
                }
            }).start();
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to perform command");
            e.printStackTrace();
            onFail(event, DEFAULT_ERROR);
        }
    }

    public abstract void doSomething(MessageEvent event) throws Exception;

    public void onFail(MessageEvent event, int error) {
        StaticStore.executed--;

        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        ch.createMessage(StaticStore.ERROR_MSG).subscribe();
    }

    public void onSuccess(MessageEvent event) {}

    public void onCancel(MessageEvent event) {}

    public MessageChannel getChannel(MessageEvent event) {
        Message msg = getMessage(event);

        return msg == null ? null : msg.getChannel().block();
    }

    public Message getMessage(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMessage");

            Object obj = m.invoke(event);

            if(obj instanceof Mono) {
                return (Message) ((Mono<?>) obj).block();
            } else if(obj instanceof Message)
                return (Message) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to get Message from this class : "+event.getClass().getName());
            e.printStackTrace();
        }

        return null;
    }

    public String getContent(MessageEvent event) {
        Message msg = getMessage(event);

        return msg == null ? null : msg.getContent();
    }

    @SuppressWarnings("unchecked")
    public Optional<Member> getMember(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getMember");

            Object obj = m.invoke(event);

            if(obj instanceof Optional)
                return (Optional<Member>) obj;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to get Message from this class : "+event.getClass().getName());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public Mono<Guild> getGuild(MessageEvent event) {
        try {
            Method m = event.getClass().getMethod("getGuild");

            Object obj = m.invoke(event);

            if(obj instanceof Mono)
                return (Mono<Guild>) obj;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            StaticStore.logger.uploadErrorLog(e, "Failed to get Message from this class : "+event.getClass().getName());
            e.printStackTrace();
        }

        return null;
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.createMessage(m -> {
            m.setContent(content);
            m.setAllowedMentions(AllowedMentions.builder().build());
        }).subscribe();
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return ch.createMessage(m -> {
            m.setContent(content);
            m.setAllowedMentions(AllowedMentions.builder().build());
        }).block();
    }
}
