package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.GuildEmojiCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.supporter.Pauser;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.SpamPrevent;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public abstract class Command {
    public static void editMessage(Message m, Consumer<MessageEditSpec.Builder> consumer) {
        MessageEditSpec.Builder builder = MessageEditSpec.builder();

        consumer.accept(builder);

        m.edit(builder.build()).subscribe();
    }

    public static void editMessage(Message m, Consumer<MessageEditSpec.Builder> consumer, Runnable postProcess) {
        MessageEditSpec.Builder builder = MessageEditSpec.builder();

        consumer.accept(builder);

        m.edit(builder.build()).subscribe();

        postProcess.run();
    }

    public static Message createMessage(MessageChannel ch, Consumer<MessageCreateSpec.Builder> consumer) {
        MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

        consumer.accept(builder);

        return ch.createMessage(builder.build()).block();
    }

    public static Message createMessage(MessageChannel ch, Consumer<MessageCreateSpec.Builder> consumer, Runnable onSuccess) {
        MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

        consumer.accept(builder);

        Message m = ch.createMessage(builder.build()).block();

        onSuccess.run();

        return m;
    }

    public static Message createMessage(MessageChannel ch, Consumer<MessageCreateSpec.Builder> consumer, Consumer<Throwable> onFailed, Runnable onSuccess) {
        try {
            MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

            consumer.accept(builder);

            Message m = ch.createMessage(builder.build()).block();

            onSuccess.run();

            return m;
        } catch (Exception e) {
            onFailed.accept(e);

            return null;
        }
    }

    public static Message createMessage(MessageChannel ch, Consumer<MessageCreateSpec.Builder> consumer, Consumer<Throwable> onFailed, Consumer<Message> onSuccess) {
        try {
            MessageCreateSpec.Builder builder = MessageCreateSpec.builder();

            consumer.accept(builder);

            Message m = ch.createMessage(builder.build()).block();

            onSuccess.accept(m);

            return m;
        } catch (Exception e) {
            onFailed.accept(e);

            return null;
        }
    }

    public static EmbedCreateSpec createEmbed(Consumer<EmbedCreateSpec.Builder> consumer) {
        EmbedCreateSpec.Builder builder = EmbedCreateSpec.builder();

        consumer.accept(builder);

        return builder.build();
    }

    public static GuildEmojiCreateSpec createEmoji(Consumer<GuildEmojiCreateSpec.Builder> consumer) {
        GuildEmojiCreateSpec.Builder builder = GuildEmojiCreateSpec.builder();

        consumer.accept(builder);

        return builder.build();
    }

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
        createMessage(ch, m -> m.content(content).allowedMentions(AllowedMentions.builder().build()));
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return createMessage(ch, m -> m.content(content).allowedMentions(AllowedMentions.builder().build()));
    }

    public Possible<Optional<String>> wrap(String content) {
        return Possible.of(Optional.of(content));
    }
}
