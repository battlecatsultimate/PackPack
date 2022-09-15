package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public abstract class MessageHolder<T extends GenericMessageEvent> implements Holder<T> {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    public boolean expired = false;

    private final Class<?> cls;
    public final Message author;

    public MessageHolder(Class<?> cls, Message author) {
        this.cls = cls;
        this.author = author;
    }

    public final long time = System.currentTimeMillis();

    public abstract int handleEvent(T event);
    public abstract void clean();
    public abstract void expire(String id);

    @Override
    public Message getAuthorMessage() {
        return author;
    }

    public boolean equals(MessageHolder<T> that) {
        return this.time == that.time;
    }

    public boolean canCastTo(Class<?> cls) {
        return this.cls != null && this.cls == cls;
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.sendMessage(content)
            .setAllowedMentions(new ArrayList<>())
            .queue();
    }

    public Message getMessageWithNoPings(MessageChannel ch, String content) {
        return ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .complete();
    }

    public void registerAutoFinish(MessageHolder<?> messageHolder, Message msg, Message author, int lang, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), messageHolder);
                msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();
            }
        }, millis);
    }

    public void registerAutoFinish(MessageHolder<?> messageHolder, Message msg, Message author, int lang, long millis, @Nullable Runnable run) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), messageHolder);
                msg.editMessage(LangID.getStringByID("formst_expire", lang)).queue();

                if(run != null)
                    run.run();
            }
        }, millis);
    }

    public void registerAutoFinish(MessageHolder<?> messageHolder, Message msg, Message author, int lang, String langID, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), messageHolder);
                msg.editMessage(LangID.getStringByID(langID, lang)).queue();
            }
        }, millis);
    }

    public void registerAutoFinish(MessageHolder<?> messageHolder, Message msg, Message author, int lang, String langID, long millis, @Nullable Runnable run) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), messageHolder);
                msg.editMessage(LangID.getStringByID(langID, lang)).queue();

                if(run != null)
                    run.run();
            }
        }, millis);
    }
}
