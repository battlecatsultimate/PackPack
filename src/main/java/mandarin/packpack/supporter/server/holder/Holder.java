package mandarin.packpack.supporter.server.holder;

import common.CommonStatic;
import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jcodec.api.NotSupportedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

    public static void registerAutoFinish(Holder holder, Message msg, long millis) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if(holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID("ui.search.expired", holder.lang))
                    .setComponents()
                    .queue();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, long millis, @Nullable Runnable run) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
        if(holder.expired)
            return;

        holder.expire(holder.userID);
        holder.expired = true;

        msg.editMessage(LangID.getStringByID("ui.search.expired", holder.lang))
                .setComponents()
                .queue();

        if(run != null)
            run.run();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, String langID, long millis) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if(holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID(langID, holder.lang))
                    .setComponents()
                    .queue();
        });
    }

    public static void registerAutoFinish(Holder holder, Message msg, String langID, long millis, @Nullable Runnable run) {
        StaticStore.executorHandler.postDelayed(millis, () -> {
            if (holder.expired)
                return;

            holder.expire(holder.userID);
            holder.expired = true;

            msg.editMessage(LangID.getStringByID(langID, holder.lang))
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
    public Message message;
    @Nonnull
    public final String userID;

    @Nonnull
    public final CommonStatic.Lang.Locale lang;

    @Nullable
    public Holder parent;

    public boolean expired = false;

    public Holder(@Nonnull Message author, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        this.author = author;

        this.channelID = channelID;
        this.message = message;
        userID = author.getAuthor().getId();

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

    public final void expire(String userID) {
        HolderHub hub = StaticStore.getHolderHub(userID);

        if(hub == null)
            throw new IllegalStateException("E/Holder::expire - Unregistered holder found : " + getClass().getName());

        onExpire(userID);

        StaticStore.removeHolder(userID, this);
    }

    public final void expire() {
        HolderHub hub = StaticStore.getHolderHub(userID);

        if(hub == null)
            throw new IllegalStateException("E/Holder::expire - Unregistered holder found : " + getClass().getName());

        onExpire(userID);

        StaticStore.removeHolder(userID, this);
    }

    public abstract void onExpire(String id);

    public abstract Type getType();

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

    public void onConnected(@Nonnull GenericComponentInteractionCreateEvent event) {

    }

    public void onConnected(@Nonnull ModalInteractionEvent event) {

    }

    public void onConnected() {

    }

    public void onBack(@Nonnull Holder child) {

    }

    public void onBack(@Nonnull GenericComponentInteractionCreateEvent event, @Nonnull Holder child) {

    }

    public void onBack(@Nonnull ModalInteractionEvent event, @Nonnull Holder child) {

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
        if (!(this instanceof MessageUpdater updater))
            return;

        if(author == null) {
            throw new NotSupportedException("E/Holder::getAuthorMessage - This holder doesn't support author message getter! : " + getClass().getName());
        }

        if (message.getChannelIdLong() != author.getChannelIdLong())
            return;

        if (message.getIdLong() != this.message.getIdLong())
            return;

        updater.onMessageUpdated(message);
    }

    public void connectTo(Holder holder) {
        if (holder.expired) {
            throw new IllegalStateException("E/Holder::connectTo - Tried to connect already expired holder!\nCurrent holder : " + this.getClass() + "\nConnected holder : " + holder.getClass());
        }

        holder.parent = this;

        if (holder.getType() == this.getType()) {
            StaticStore.removeHolder(userID, this);
        }

        StaticStore.putHolder(userID, holder);

        holder.onConnected();
    }

    public void connectTo(@Nonnull ModalInteractionEvent event, Holder holder) {
        if (holder.expired) {
            throw new IllegalStateException("E/Holder::connectTo - Tried to connect already expired holder!\nCurrent holder : " + this.getClass() + "\nConnected holder : " + holder.getClass());
        }

        holder.parent = this;

        if (holder.getType() == this.getType()) {
            StaticStore.removeHolder(userID, this);
        }

        StaticStore.putHolder(userID, holder);

        holder.onConnected(event);
    }

    public void connectTo(@Nonnull GenericComponentInteractionCreateEvent event, Holder holder) {
        if (holder.expired) {
            throw new IllegalStateException("E/Holder::connectTo - Tried to connect already expired holder!\nCurrent holder : " + this.getClass() + "\nConnected holder : " + holder.getClass());
        }

        holder.parent = this;

        if (holder.getType() == this.getType()) {
            StaticStore.removeHolder(userID, this);
        }

        StaticStore.putHolder(userID, holder);

        holder.onConnected(event);
    }

    public void goBack() {
        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBack - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else if (parent.expired) {
            throw new IllegalStateException("E/Holder::goBack - Parent holder " + parent.getClass() + " is already expired!\n\nHolder : " + this.getClass());
        } else {
            expired = true;
            StaticStore.removeHolder(userID, this);
            StaticStore.putHolder(userID, parent);

            if (parent instanceof MessageUpdater updater) {
                System.out.println("BACK : " + message.getAttachments());

                updater.onMessageUpdated(message);
            }

            Objects.requireNonNull(parent).onBack(this);
        }
    }

    public void goBack(GenericComponentInteractionCreateEvent event) {
        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBack - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else if (parent.expired) {
            throw new IllegalStateException("E/Holder::goBack - Parent holder " + parent.getClass() + " is already expired!\n\nHolder : " + this.getClass());
        } else {
            expired = true;
            StaticStore.removeHolder(userID, this);
            StaticStore.putHolder(userID, parent);

            if (parent instanceof MessageUpdater updater) {
                updater.onMessageUpdated(message);
            }

            Objects.requireNonNull(parent).onBack(event, this);
        }
    }

    public void goBack(ModalInteractionEvent event) {
        if (parent == null) {
            throw new IllegalStateException("E/Holder::goBack - Can't go back because there's no parent holder!\n\nHolder : " + this.getClass());
        } else if (parent.expired) {
            throw new IllegalStateException("E/Holder::goBack - Parent holder " + parent.getClass() + " is already expired!\n\nHolder : " + this.getClass());
        } else {
            expired = true;
            StaticStore.removeHolder(userID, this);
            StaticStore.putHolder(userID, parent);

            if (parent instanceof MessageUpdater updater) {
                updater.onMessageUpdated(message);
            }

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

                    expired = true;

                    StaticStore.removeHolder(userID, this);
                    StaticStore.putHolder(userID, parent);

                    if (parent instanceof MessageUpdater updater) {
                        updater.onMessageUpdated(message);
                    }

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

    public void goBackTo(@Nonnull GenericComponentInteractionCreateEvent event, Class<?> parentClass) {
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

                    expired = true;

                    StaticStore.removeHolder(userID, this);
                    StaticStore.putHolder(userID, parent);

                    if (parent instanceof MessageUpdater updater) {
                        updater.onMessageUpdated(message);
                    }

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

    public void registerPopUp(GenericComponentInteractionCreateEvent event, String content) {
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

    public void registerPopUp(ModalInteractionEvent event, String content) {
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
}
