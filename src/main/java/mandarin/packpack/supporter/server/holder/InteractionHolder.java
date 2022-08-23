package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class InteractionHolder<T extends GenericInteractionCreateEvent> implements Holder<T> {
    public boolean expired = false;

    private final Class<?> cls;
    private final Message author;

    public InteractionHolder(Class<?> cls, Message author) {
        this.cls = cls;
        this.author = author;
    }

    public final long time = System.currentTimeMillis();

    @Override
    public abstract int handleEvent(T event);

    public abstract void performInteraction(T event);

    @Override
    public abstract void clean();

    @Override
    public abstract void expire(String id);

    @Override
    public Message getAuthorMessage() {
        return author;
    }

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

    public void registerAutoFinish(Holder<?> holder, Message msg, Message author, int lang, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), holder);
                msg.editMessage(LangID.getStringByID("formst_expire", lang))
                        .setActionRows()
                        .queue();
            }
        }, millis);
    }

    public void registerAutoFinish(Holder<?> holder, Message msg, Message author, int lang, long millis, @Nullable Runnable run) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), holder);
                msg.editMessage(LangID.getStringByID("formst_expire", lang))
                        .setActionRows()
                        .queue();

                if(run != null)
                    run.run();
            }
        }, millis);
    }

    public void registerAutoFinish(Holder<?> holder, Message msg, Message author, int lang, String langID, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), holder);
                msg.editMessage(LangID.getStringByID(langID, lang))
                        .setActionRows()
                        .queue();
            }
        }, millis);
    }

    public void registerAutoFinish(Holder<?> holder, Message msg, Message author, int lang, String langID, long millis, @Nullable Runnable run) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), holder);
                msg.editMessage(LangID.getStringByID(langID, lang))
                        .setActionRows()
                        .queue();

                if(run != null)
                    run.run();
            }
        }, millis);
    }

    public int parseDataToInt(GenericComponentInteractionCreateEvent event) {
        if(!(event instanceof SelectMenuInteractionEvent)) {
            throw new IllegalStateException("Event type isn't SelectMenuInteractionEvent!");
        }

        return StaticStore.safeParseInt(((SelectMenuInteractionEvent) event).getValues().get(0));
    }
}
