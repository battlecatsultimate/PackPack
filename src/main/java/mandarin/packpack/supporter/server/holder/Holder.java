package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jcodec.api.NotSupportedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class Holder {
    public enum STATUS {
        WAIT,
        FINISH,
        FAIL
    }

    public enum Type {
        MESSAGE,
        COMPONENT,
        MODAL
    }

    protected static final long FIVE_MIN = TimeUnit.MINUTES.toMillis(5);

    public static int getTotalPage(int size) {
        return (int) Math.ceil(size * 1.0 / SearchHolder.PAGE_CHUNK);
    }

    public static int getTotalPage(int size, int chunk) {
        return (int) Math.ceil(size * 1.0 / chunk);
    }

    public final long time = System.currentTimeMillis();
    @Nullable
    private final Message author;
    @Nonnull
    public final String channelID;
    @Nonnull
    public Message message;
    @Nonnull
    public final String userID;

    @Nonnull
    public final CommonStatic.Lang.Locale lang;

    @Nullable
    public Holder parent;
    @Nullable
    public Holder child;

    public boolean isRoot = true;

    private boolean expired = false;

    @Nullable
    private ScheduledFuture<?> schedule = null;

    public Holder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        this.author = author;

        this.channelID = channelID;
        this.message = message;
        this.userID = userID;

        this.lang = lang;
    }

    public Holder(@Nonnull GenericCommandInteractionEvent event, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        this.author = null;

        String channelID = event.getChannelId();

        if (channelID == null) {
            throw new NullPointerException("E/Holder::init - Failed to get channel data from slash command");
        }

        this.channelID = channelID;
        this.message = message;

        userID = event.getUser().getId();

        this.lang = lang;
    }

    public abstract STATUS handleEvent(Event event);

    public abstract void clean();

    public final void expire() {
        try {
            expired = true;

            if (schedule != null) {
                schedule.cancel(true);
            }

            HolderHub hub = StaticStore.getHolderHub(userID);

            if(hub == null)
                throw new IllegalStateException("E/Holder::expire - Unregistered holder found : " + getClass().getName());

            if (isRoot) {
                Holder childHolder = child;

                while (childHolder != null) {
                    childHolder.expired = true;

                    if (childHolder.schedule != null) {
                        childHolder.schedule.cancel(true);
                    }

                    childHolder = childHolder.child;
                }
            }

            onExpire();

            StaticStore.removeHolder(userID, this);
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/Holder::expire - Failed to perform holder expiration");
        }
    }

    public final void end(boolean finishAll) {
        expired = true;

        HolderHub hub = StaticStore.getHolderHub(userID);

        if(hub == null)
            throw new IllegalStateException("E/Holder::expire - Unregistered holder found : " + getClass().getName());

        if (schedule != null) {
            schedule.cancel(true);
        }

        Holder childHolder = child;

        while (childHolder != null) {
            childHolder.expired = true;

            if (childHolder.schedule != null) {
                childHolder.schedule.cancel(true);
            }

            StaticStore.removeHolder(userID, childHolder);
            childHolder = childHolder.child;
        }

        if (finishAll) {
            Holder parentHolder = parent;

            while (parentHolder != null) {
                parentHolder.expired = true;

                if (parentHolder.schedule != null) {
                    parentHolder.schedule.cancel(true);
                }

                StaticStore.removeHolder(userID, childHolder);
                parentHolder = parentHolder.parent;
            }
        }

        StaticStore.removeHolder(userID, this);
    }

    public abstract void onExpire();

    public abstract Type getType();

    @Nonnull
    public final Message getAuthorMessage() {
        if(author == null) {
            throw new NotSupportedException("E/Holder::getAuthorMessage - This holder doesn't support author message getter! : " + getClass().getName());
        }

        return author;
    }

    public final boolean hasAuthorMessage() {
        return author != null;
    }

    public boolean equals(Holder that) {
        return this.time == that.time;
    }

    public void createMessageWithNoPings(MessageChannel ch, String content) {
        ch.sendMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .queue();
    }

    public void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {
        throw new UnsupportedOperationException("E/Holder::onConnected - Unhandled connection\n" + parent.getClass() + " -> " + this.getClass());
    }

    public void onConnected(Holder parent) {
        throw new UnsupportedOperationException("E/Holder::onConnected - Unhandled connection\n" + parent.getClass() + " -> " + this.getClass());
    }

    public void onBack(@Nonnull Holder child) {
        throw new UnsupportedOperationException("E/Holder::onBack - Unhandled back handler\n" + child.getClass() + " -> " + this.getClass());
    }

    public void onBack(@Nonnull IMessageEditCallback event, @Nonnull Holder child) {
        throw new UnsupportedOperationException("E/Holder::onBack - Unhandled back handler\n" + child.getClass() + " -> " + this.getClass());
    }

    public void handleMessageDetected(@Nonnull Message message) {
        if (!(this instanceof MessageDetector detector))
            return;

        if(author == null) {
            throw new NotSupportedException("E/Holder::getAuthorMessage - This holder doesn't support author message getter! : " + getClass().getName());
        }

        if (message.getChannelIdLong() != author.getChannelIdLong())
            return;

        if (message.getAuthor().getIdLong() != author.getAuthor().getIdLong())
            return;

        detector.onMessageDetected(message);
    }

    public void handleMessageUpdated(@Nonnull Message message) {
        if (parent != null) {
            parent.handleMessageUpdated(message);
        }

        if(author == null) {
            throw new NotSupportedException("E/Holder::getAuthorMessage - This holder doesn't support author message getter! : " + getClass().getName());
        }

        if (message.getChannelIdLong() != author.getChannelIdLong())
            return;

        if (message.getIdLong() != this.message.getIdLong())
            return;

        this.message = message;
    }

    public void connectTo(Holder holder) {
        if (holder.expired) {
            throw new IllegalStateException("E/Holder::connectTo - Tried to connect already expired holder!\nCurrent holder : " + this.getClass() + "\nConnected holder : " + holder.getClass());
        }

        holder.parent = this;
        child = holder;

        if (holder.getType() == this.getType()) {
            StaticStore.removeHolder(userID, this);
        }

        StaticStore.putHolder(userID, holder);

        holder.isRoot = false;
        holder.onConnected(this);
    }

    public void connectTo(@Nonnull IMessageEditCallback event, Holder holder) {
        if (holder.expired) {
            throw new IllegalStateException("E/Holder::connectTo - Tried to connect already expired holder!\nCurrent holder : " + this.getClass() + "\nConnected holder : " + holder.getClass());
        }

        holder.parent = this;
        child = holder;

        if (holder.getType() == this.getType()) {
            StaticStore.removeHolder(userID, this);
        }

        StaticStore.putHolder(userID, holder);

        holder.isRoot = false;
        holder.onConnected(event, this);
    }

    public void goBack() {
        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBack - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else if (parent.expired) {
            throw new IllegalStateException("E/Holder::goBack - Parent holder " + parent.getClass() + " is already expired!\n\nHolder : " + this.getClass());
        } else {
            Holder childHolder = child;

            while (childHolder != null) {
                childHolder.expired = true;

                if (childHolder.schedule != null) {
                    childHolder.schedule.cancel(true);
                }

                childHolder = childHolder.child;
            }

            if (schedule != null) {
                schedule.cancel(true);
            }

            StaticStore.removeHolder(userID, this);
            StaticStore.putHolder(userID, parent);

            Objects.requireNonNull(parent).onBack(this);
        }
    }

    public void goBack(IMessageEditCallback event) {
        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBack - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else if (parent.expired) {
            throw new IllegalStateException("E/Holder::goBack - Parent holder " + parent.getClass() + " is already expired!\n\nHolder : " + this.getClass());
        } else {
            Holder childHolder = child;

            while (childHolder != null) {
                childHolder.expired = true;

                if (childHolder.schedule != null) {
                    childHolder.schedule.cancel(true);
                }

                childHolder = childHolder.child;
            }

            if (schedule != null) {
                schedule.cancel(true);
            }

            StaticStore.removeHolder(userID, this);
            StaticStore.putHolder(userID, parent);

            Objects.requireNonNull(parent).onBack(event, this);
        }
    }

    public void goBackTo(Class<?> parentClass) {
        Holder parent = this.parent;

        List<Holder> skimmedHolder = new ArrayList<>();

        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBackTo - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else {
            while(parent != null) {
                if (parent.getClass() == parentClass) {
                    if (parent.expired) {
                        skimmedHolder.add(parent);

                        parent = parent.parent;

                        continue;
                    }

                    for (Holder parentHolder : skimmedHolder) {
                        parentHolder.expired = true;

                        if (parentHolder.schedule != null) {
                            parentHolder.schedule.cancel(true);
                        }
                    }

                    Holder childHolder = child;

                    while (childHolder != null) {
                        childHolder.expired = true;

                        if (childHolder.schedule != null) {
                            childHolder.schedule.cancel(true);
                        }

                        childHolder = childHolder.child;
                    }

                    if (schedule != null) {
                        schedule.cancel(true);
                    }

                    StaticStore.removeHolder(userID, this);
                    StaticStore.putHolder(userID, parent);

                    parent.onBack(this);

                    return;
                } else {
                    skimmedHolder.add(parent);

                    parent = parent.parent;
                }
            }

            StringBuilder errorMessage = new StringBuilder("E/Holder::goBackTo - Failed to find parent holder in this hierarchy!\n\nTargeted Holder : ")
                    .append(parentClass.getName())
                    .append("\nHierarchy : \n```");

            for (int i = 0; i < skimmedHolder.size(); i++) {
                errorMessage.append("    ".repeat(i)).append("- ").append(skimmedHolder.get(i).getClass()).append("\n");

                if (i < skimmedHolder.size() - 1) {
                    errorMessage.append("    ".repeat(i)).append("   |\n");
                }
            }

            throw new IllegalStateException(errorMessage.toString());
        }
    }

    public void goBackTo(@Nonnull IMessageEditCallback event, Class<?> parentClass) {
        Holder parent = this.parent;

        List<Holder> skimmedHolder = new ArrayList<>();

        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBackTo - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else {
            while(parent != null) {
                if (parent.getClass() == parentClass) {
                    if (parent.expired) {
                        skimmedHolder.add(parent);

                        parent = parent.parent;

                        continue;
                    }

                    for (Holder parentHolder : skimmedHolder) {
                        parentHolder.expired = true;

                        if (parentHolder.schedule != null) {
                            parentHolder.schedule.cancel(true);
                        }
                    }

                    Holder childHolder = child;

                    while (childHolder != null) {
                        childHolder.expired = true;

                        if (childHolder.schedule != null) {
                            childHolder.schedule.cancel(true);
                        }

                        childHolder = childHolder.child;
                    }

                    if (schedule != null) {
                        schedule.cancel(true);
                    }

                    StaticStore.removeHolder(userID, this);
                    StaticStore.putHolder(userID, parent);

                    parent.onBack(event, this);

                    return;
                } else {
                    skimmedHolder.add(parent);

                    parent = parent.parent;
                }
            }

            StringBuilder errorMessage = new StringBuilder("E/Holder::goBackTo - Failed to find parent holder in this hierarchy!\n\nTargeted Holder : ")
                    .append(parentClass.getName())
                    .append("\nHierarchy : \n```");

            for (int i = 0; i < skimmedHolder.size(); i++) {
                errorMessage.append("    ".repeat(i)).append("- ").append(skimmedHolder.get(i).getClass()).append("\n");

                if (i < skimmedHolder.size() - 1) {
                    errorMessage.append("    ".repeat(i)).append("   |\n");
                }
            }

            throw new IllegalStateException(errorMessage.toString());
        }
    }

    public void registerPopUp(IMessageEditCallback event, String content) {
        event.deferEdit()
                .setContent(content)
                .setAllowedMentions(new ArrayList<>())
                .setFiles()
                .mentionRepliedUser(false)
                .setComponents(
                        ActionRow.of(
                                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                        )
                )
                .queue();
    }

    public void registerPopUp(Message message, String content) {
        message.editMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setFiles()
                .setComponents(
                        ActionRow.of(
                                Button.success("confirm", LangID.getStringByID("ui.button.confirm", lang)).withEmoji(EmojiStore.CHECK),
                                Button.danger("cancel", LangID.getStringByID("ui.button.cancel", lang)).withEmoji(EmojiStore.CROSS)
                        )
                )
                .queue();
    }

    public void registerAutoExpiration(long delayedTime) {
        schedule = StaticStore.executorHandler.postDelayed(delayedTime, () -> {
            if(expired) {
                StaticStore.logger.uploadLog("W/Holder::registerAutoExpiration - Bot found expired holder being auto-expired. Maybe forgot to cancel the schedule?\nHolder = " + this.getClass());

                return;
            }

            expire();

            StaticStore.removeHolder(userID, this);
        });
    }

    /**
     * Register automatic expiration
     * @param code Language data ID or message contents
     * @param raw Whether bot will use passed {@code code} will be used as raw contents or not
     * @param delayedTime Delay of expiration
     */
    public void registerAutoExpiration(String code, boolean raw, long delayedTime) {
        schedule = StaticStore.executorHandler.postDelayed(delayedTime, () -> {
            if(expired) {
                StaticStore.logger.uploadLog("W/Holder::registerAutoExpiration - Bot found expired holder being auto-expired. Maybe forgot to cancel the schedule?\nHolder = " + this.getClass());

                return;
            }

            expired = true;

            message.editMessage(raw ? code : LangID.getStringByID(code, lang))
                    .setComponents()
                    .setAllowedMentions(new ArrayList<>())
                    .mentionRepliedUser(false)
                    .queue();

            StaticStore.removeHolder(userID, this);
        });
    }
}
