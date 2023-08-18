package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import org.jcodec.api.NotSupportedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public abstract class Holder {
    public enum STATUS {
        WAIT,
        FINISH,
        FAIL
    }

    protected static final long FIVE_MIN = TimeUnit.MINUTES.toMillis(5);

    public static void registerAutoFinish(Holder holder, Message msg, int lang, long millis) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if(holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID("formst_expire", lang))
                    .setComponents()
                    .queue();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, int lang, long millis, @Nullable Runnable run) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
        if(holder.expired)
            return;

        holder.expire(holder.userID);
        holder.expired = true;

        msg.editMessage(LangID.getStringByID("formst_expire", lang))
                .setComponents()
                .queue();

        if(run != null)
            run.run();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, int lang, String langID, long millis) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if(holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID(langID, lang))
                    .setComponents()
                    .queue();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, int lang, String langID, long millis, @Nullable Runnable run) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if (holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID(langID, lang))
                    .setComponents()
                    .queue();

            if (run != null)
                run.run();
        });
    }

    public final long time = System.currentTimeMillis();
    @Nullable
    private final Message author;
    @Nonnull
    public final String channelID;
    @Nonnull
    public final String messageID;
    @Nonnull
    public final String userID;

    public boolean expired = false;

    public Holder(@Nonnull Message author, @Nonnull String channelID, @Nonnull String messageID) {
        this.author = author;

        this.channelID = channelID;
        this.messageID = messageID;
        userID = author.getAuthor().getId();
    }

    public Holder(@Nonnull String channelID, @Nonnull String messageID, @Nonnull String userID) {
        author = null;

        this.channelID = channelID;
        this.messageID = messageID;
        this.userID = userID;
    }

    public abstract STATUS handleEvent(Event event);

    public abstract void clean();
    public final void expire(String userID) {
        HolderHub hub = StaticStore.getHolderHub(userID);

        if(hub == null)
            throw new IllegalStateException("E/Holder::expire - Unregistered holder found : " + getClass().getName());

        onExpire(userID);

        StaticStore.removeHolder(userID, this);
    }

    public abstract void onExpire(String id);

    @Nonnull
    public final Message getAuthorMessage() {
        if(author == null) {
            throw new NotSupportedException("E/Holder::getAuthorMessage - This holder doesn't support author message getter! : " + getClass().getName());
        }

        return author;
    }

    public boolean equals(Holder that) {
        return this.time == that.time;
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }
}
