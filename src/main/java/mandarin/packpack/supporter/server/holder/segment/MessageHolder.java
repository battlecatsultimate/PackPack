package mandarin.packpack.supporter.server.holder.segment;

import io.opencensus.trace.MessageEvent;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
                && event.getMessage().getId().equals(messageID)
                && event.getAuthor().getId().equals(userID);
    }

    private boolean canHandleEvent(MessageReactionAddEvent event) {
        return event.getChannel().getId().equals(channelID)
                && event.getMessageId().equals(messageID)
                && event.getUserId().equals(userID);
    }
}