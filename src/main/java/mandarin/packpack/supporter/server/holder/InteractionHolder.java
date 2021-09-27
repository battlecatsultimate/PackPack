package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.Command;
import reactor.core.publisher.Mono;

import java.util.Optional;

public abstract class InteractionHolder<T extends InteractionCreateEvent> implements Holder<T> {
    public boolean expired = false;

    private final Class<?> cls;

    public InteractionHolder(Class<?> cls) {
        this.cls = cls;
    }

    public final long time = System.currentTimeMillis();

    @Override
    public abstract int handleEvent(T event);

    public abstract Mono<?> getInteraction(T event);

    @Override
    public abstract void clean();

    @Override
    public abstract void expire(String id);

    public boolean equals(InteractionHolder<T> that) {
        return this.time == that.time;
    }

    public boolean canCastTo(Class<?> cls) {
        return this.cls != null && this.cls == cls;
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        Command.createMessage(ch, m -> {
            m.content(content);
            m.allowedMentions(AllowedMentions.builder().build());
        });
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return Command.createMessage(ch, m -> {
            m.content(content);
            m.allowedMentions(AllowedMentions.builder().build());
        });
    }

    public Possible<Optional<String>> wrap(String content) {
        return Possible.of(Optional.of(content));
    }
}
