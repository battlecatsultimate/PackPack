package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.EmojiStore;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jcodec.api.NotSupportedException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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

    @Nullable
    public Holder parent;

    public boolean expired = false;

    public Holder(@Nonnull Message author, @Nonnull String channelID, @Nonnull String messageID) {
        this.author = author;

        this.channelID = channelID;
        this.messageID = messageID;
        userID = author.getAuthor().getId();
    }

    public Holder(@Nonnull Message author, @Nonnull String channelID, @Nonnull Message message) {
        this.author = author;

        this.channelID = channelID;
        this.messageID = message.getId();
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

    public void onBack() {

    }

    public void onBack(@Nonnull GenericComponentInteractionCreateEvent event) {

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

            parent.onBack();
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

            parent.onBack(event);
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

                    parent.onBack();

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

                    parent.onBack(event);

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

    public void registerPopUp(GenericComponentInteractionCreateEvent event, String content, int lang) {
        event.deferEdit()
                .setContent(content)
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents(
                        ActionRow.of(
                                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                        )
                )
                .queue();
    }

    public void registerPopUp(ModalInteractionEvent event, String content, int lang) {
        event.deferEdit()
                .setContent(content)
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents(
                        ActionRow.of(
                                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                        )
                )
                .queue();
    }

    public void registerPopUp(Message message, String content, int lang) {
        message.editMessage(content)
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .setComponents(
                        ActionRow.of(
                                Button.success("confirm", LangID.getStringByID("button_confirm", lang)).withEmoji(EmojiStore.CHECK),
                                Button.danger("cancel", LangID.getStringByID("button_cancel", lang)).withEmoji(EmojiStore.CROSS)
                        )
                )
                .queue();
    }
}
