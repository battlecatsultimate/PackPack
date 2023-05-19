package mandarin.packpack.supporter.server.holder.message;

import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import javax.annotation.Nonnull;

public abstract class MessageHolder extends Holder {
    public MessageHolder(@Nonnull Message author, @Nonnull String channelID, @Nonnull String messageID) {
        super(author, channelID, messageID);
    }

    public MessageHolder(@Nonnull String channelID, @Nonnull String messageID, @Nonnull String userID) {
        super(channelID, messageID, userID);
    }

    @Override
    public final STATUS handleEvent(Event event) {
        if(event instanceof MessageReceivedEvent receivedEvent && canHandleEvent(receivedEvent)) {
            return onReceivedEvent(receivedEvent);
        } else if(event instanceof MessageReactionAddEvent reactionAddEvent && canHandleEvent(reactionAddEvent)) {
            return onReactionEvent(reactionAddEvent);
        }

        return STATUS.WAIT;
    }

    public STATUS onReceivedEvent(MessageReceivedEvent event) {
        return STATUS.WAIT;
    }

    public STATUS onReactionEvent(MessageReactionAddEvent event) {
        return STATUS.WAIT;
    }

    private boolean canHandleEvent(MessageReceivedEvent event) {
        return event.getChannel().getId().equals(channelID)
                && event.getAuthor().getId().equals(userID);
    }

    private boolean canHandleEvent(MessageReactionAddEvent event) {
        return event.getChannel().getId().equals(channelID)
                && event.getMessageId().equals(messageID)
                && event.getUserId().equals(userID);
    }
}
