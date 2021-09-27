package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.possible.Possible;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public abstract class MessageHolder<T extends MessageEvent> implements Holder<T> {
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    public boolean expired = false;

    private final Class<?> cls;

    public MessageHolder(Class<?> cls) {
        this.cls = cls;
    }

    public final long time = System.currentTimeMillis();

    public abstract int handleEvent(T event);
    public abstract void clean();
    public abstract void expire(String id);

    public boolean equals(MessageHolder<T> that) {
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

    public void registerAutoFinish(MessageHolder<?> messageHolder, Message msg, Message author, int lang, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), messageHolder));

                Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));
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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), messageHolder));

                Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID("formst_expire", lang))));

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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), messageHolder));

                Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID(langID, lang))));
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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), messageHolder));

                Command.editMessage(msg, m -> m.content(wrap(LangID.getStringByID(langID, lang))));

                if(run != null)
                    run.run();
            }
        }, millis);
    }

    public Possible<Optional<String>> wrap(String content) {
        return Possible.of(Optional.of(content));
    }
}
