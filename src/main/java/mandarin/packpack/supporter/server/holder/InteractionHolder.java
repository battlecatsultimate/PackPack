package mandarin.packpack.supporter.server.holder;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;

import java.util.ArrayList;

public abstract class InteractionHolder<T extends GenericInteractionCreateEvent> implements Holder<T> {
    public boolean expired = false;

    private final Class<?> cls;

    public InteractionHolder(Class<?> cls) {
        this.cls = cls;
    }

    public final long time = System.currentTimeMillis();

    @Override
    public abstract int handleEvent(T event);

    public abstract void performInteraction(T event);

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
        ch.sendMessage(content)
                .allowedMentions(new ArrayList<>())
                .queue();
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return ch.sendMessage(content)
                .allowedMentions(new ArrayList<>())
                .complete();
    }
}
