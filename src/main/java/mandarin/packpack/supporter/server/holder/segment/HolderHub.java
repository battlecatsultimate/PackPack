package mandarin.packpack.supporter.server.holder.segment;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class HolderHub {
    public String userID;

    public MessageHolder messageHolder;
    public InteractionHolder interactionHolder;

    public void handleEvent(Event e) {
        try {
            if(e instanceof GenericMessageEvent && messageHolder != null) {
                if(messageHolder.expired) {
                    StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired message holder didn't get removed : " + messageHolder.getClass().getName());

                    messageHolder = null;
                } else {
                    Holder.STATUS result = messageHolder.handleEvent(e);

                    if(result == Holder.STATUS.FINISH || result == Holder.STATUS.FAIL) {
                        messageHolder.clean();
                        messageHolder = null;
                    }
                }
            } else if(e instanceof GenericInteractionCreateEvent && interactionHolder != null) {
                if(interactionHolder.expired) {
                    StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired interaction holder didn't get removed : " + interactionHolder.getClass().getName());

                    interactionHolder = null;
                } else {
                    Holder.STATUS result = interactionHolder.handleEvent(e);

                    if(result == Holder.STATUS.FINISH && interactionHolder != null && interactionHolder.expired) {
                        interactionHolder = null;
                    }
                }
            }
        } catch (Exception exception) {
            StaticStore.logger.uploadErrorLog(exception, "E/HolderHub::handleEvent - Failed to handle the event");
        }
    }
}
