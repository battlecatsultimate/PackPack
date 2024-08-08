package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import mandarin.packpack.supporter.server.holder.message.MessageHolder;
import mandarin.packpack.supporter.server.holder.modal.ModalHolder;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class HolderHub {
    public MessageHolder messageHolder;
    public ComponentHolder componentHolder;
    public ModalHolder modalHolder;

    public void handleEvent(Event event) {
        switch (event) {
            case GenericMessageEvent ge -> {
                if (messageHolder == null)
                    return;

                try {
                    Holder.STATUS result = messageHolder.handleEvent(ge);

                    if (result == Holder.STATUS.FAIL) {
                        messageHolder.clean();

                        messageHolder = null;
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle message holder\n" +
                                    "\n" +
                                    "Holder = " + (messageHolder == null ? "None" : messageHolder.getClass().getName()) + "\n" +
                                    "Command = " + (messageHolder == null ? "None" : messageHolder.getAuthorMessage().getContentRaw())
                    );
                }
            }
            case GenericComponentInteractionCreateEvent gie -> {
                if (componentHolder == null)
                    return;

                try {
                    Holder.STATUS result = componentHolder.handleEvent(gie);

                    if (result == Holder.STATUS.FAIL) {
                        componentHolder = null;
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle component holder\n" +
                                    "\n" +
                                    "Holder = " + (componentHolder == null ? "None" : componentHolder.getClass().getName()) + "\n" +
                                    "Command = " + (componentHolder == null ? "None" : componentHolder.getAuthorMessage().getContentRaw())
                    );
                }
            }
            case ModalInteractionEvent mie -> {
                if (modalHolder == null)
                    return;

                try {
                    Holder.STATUS result = modalHolder.handleEvent(mie);

                    if (result == Holder.STATUS.FAIL) {
                        modalHolder = null;
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle modal holder\n" +
                                    "\n" +
                                    "Holder = " + (modalHolder == null ? "None" : modalHolder.getClass().getName()) + "\n" +
                                    "Command = " + (modalHolder == null ? "None" : modalHolder.getAuthorMessage().getContentRaw())
                    );
                }
            }
            default -> { }
        }
    }

    public void handleMessageDelete(String messageID) {
        if (messageHolder != null && messageHolder.message.getId().equals(messageID)) {
            messageHolder.end(true);
        }

        if (componentHolder != null && componentHolder.message.getId().equals(messageID)) {
            componentHolder.end(true);
        }

        if (modalHolder != null && modalHolder.message.getId().equals(messageID)) {
            modalHolder.end(true);
        }
    }

    public void handleChannelDelete(String channelID) {
        if (messageHolder != null && messageHolder.channelID.equals(channelID)) {
            messageHolder.end(true);
        }

        if (componentHolder != null && componentHolder.channelID.equals(channelID)) {
            componentHolder.end(true);
        }

        if (modalHolder != null && modalHolder.channelID.equals(channelID)) {
            modalHolder.end(true);
        }
    }
}
