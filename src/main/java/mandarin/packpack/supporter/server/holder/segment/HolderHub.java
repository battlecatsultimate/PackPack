package mandarin.packpack.supporter.server.holder.segment;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class HolderHub {
    public MessageHolder messageHolder;
    public ComponentHolder componentHolder;
    public ModalHolder modalHolder;

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
            } else if(e instanceof GenericComponentInteractionCreateEvent && componentHolder != null) {
                if(componentHolder.expired) {
                    StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired interaction holder didn't get removed : " + componentHolder.getClass().getName());

                    componentHolder = null;
                } else {
                    Holder.STATUS result = componentHolder.handleEvent(e);

                    if(result == Holder.STATUS.FINISH && componentHolder != null && componentHolder.expired) {
                        componentHolder = null;
                    }
                }
            } else if(e instanceof ModalInteractionEvent && modalHolder != null) {
                if(modalHolder.expired) {
                    StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired modal holder didn't get removed : " + modalHolder.getClass().getName());

                    modalHolder = null;
                } else {
                    Holder.STATUS result = modalHolder.handleEvent(e);

                    if(result == Holder.STATUS.FINISH && modalHolder != null && modalHolder.expired) {
                        modalHolder = null;
                    }
                }
            }
        } catch (Exception exception) {
            String message = "E/HolderHub::handleEvent - Failed to handle the event\n\n";

            message += "MessageHolder : " + (messageHolder == null ? "None" : messageHolder.getClass().getName());
            message += "ComponentHolder : " + (componentHolder == null ? "None" : componentHolder.getClass().getName());
            message += "ModalHolder : " + (modalHolder == null ? "None" : modalHolder.getClass().getName());

            StaticStore.logger.uploadErrorLog(exception, message);
        }
    }
}
