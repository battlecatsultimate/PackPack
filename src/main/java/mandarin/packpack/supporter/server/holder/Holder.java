package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.rest.util.AllowedMentions;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;

import javax.annotation.Nullable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public abstract class Holder<T extends MessageEvent> {
    public static final long FIVE_MIN = TimeUnit.MINUTES.toMillis(5);

    public static final int RESULT_FAIL = -1;
    public static final int RESULT_STILL = 0;
    public static final int RESULT_FINISH = 1;

    public boolean expired = false;

    private final Class<?> cls;

    public Holder(Class<?> cls) {
        this.cls = cls;
    }

    public final long time = System.currentTimeMillis();

    public abstract int handleEvent(T event);
    public abstract void clean();
    public abstract void expire(String id);

    public boolean equals(Holder<T> that) {
        return this.time == that.time;
    }

    public boolean canCastTo(Class<?> cls) {
        return this.cls != null && this.cls == cls;
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

    public void registerAutoFinish(Holder<?> holder, Message msg, Message author, int lang, long millis) {
        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), holder));

                msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();
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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), holder));

                msg.edit(m -> m.setContent(LangID.getStringByID("formst_expire", lang))).subscribe();

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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), holder));

                msg.edit(m -> m.setContent(LangID.getStringByID(langID, lang))).subscribe();
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

                author.getAuthor().ifPresent(au -> StaticStore.removeHolder(au.getId().asString(), holder));

                msg.edit(m -> m.setContent(LangID.getStringByID(langID, lang))).subscribe();

                if(run != null)
                    run.run();
            }
        }, millis);
    }
}
