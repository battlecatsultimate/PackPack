package mandarin.packpack.supporter.server.holder.message;

import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import javax.annotation.Nonnull;

public abstract class MessageHolder extends Holder {
    public MessageHolder(@Nonnull Message author, @Nonnull String channelID, @Nonnull Message message) {
        super(author, channelID, message);
    }

    public MessageHolder(@Nonnull GenericCommandInteractionEvent event, @Nonnull Message message) {
        super(event, message);
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

    @Override
    public final Type getType() {
        return Type.MESSAGE;
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
                && event.getMessageId().equals(message.getId())
                && event.getUserId().equals(userID);
    }
}
