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
                    if (messageHolder.expired) {
                        StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired message holder didn't get removed : " + messageHolder.getClass().getName());

                        messageHolder = null;
                    } else {
                        Holder.STATUS result = messageHolder.handleEvent(ge);

                        if (result == Holder.STATUS.FINISH || result == Holder.STATUS.FAIL) {
                            messageHolder.clean();
                            messageHolder = null;
                        }
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle message holder\n" +
                                    "\n" +
                                    "Holder = " + messageHolder.getClass().getName() + "\n" +
                                    "Command = " + messageHolder.getAuthorMessage().getContentRaw()
                    );
                }
            }
            case GenericComponentInteractionCreateEvent gie -> {
                if (componentHolder == null)
                    return;

                try {
                    if (componentHolder.expired) {
                        StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired interaction holder didn't get removed : " + componentHolder.getClass().getName());

                        componentHolder = null;
                    } else {
                        Holder.STATUS result = componentHolder.handleEvent(gie);

                        if (result == Holder.STATUS.FINISH && componentHolder != null && componentHolder.expired) {
                            componentHolder = null;
                        }
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle component holder\n" +
                                    "\n" +
                                    "Holder = " + componentHolder.getClass().getName() + "\n" +
                                    "Command = " + componentHolder.getAuthorMessage().getContentRaw()
                    );
                }
            }
            case ModalInteractionEvent mie -> {
                if (modalHolder == null)
                    return;

                try {
                    if (modalHolder.expired) {
                        StaticStore.logger.uploadLog("W/HolderHub::handleEvent - Expired modal holder didn't get removed : " + modalHolder.getClass().getName());

                        modalHolder = null;
                    } else {
                        Holder.STATUS result = modalHolder.handleEvent(mie);

                        if (result == Holder.STATUS.FINISH && modalHolder != null && modalHolder.expired) {
                            modalHolder = null;
                        }
                    }
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e,
                            "E/HolderHub::handleEvent - Failed to handle modal holder\n" +
                                    "\n" +
                                    "Holder = " + modalHolder.getClass().getName() + "\n" +
                                    "Command = " + modalHolder.getAuthorMessage().getContentRaw()
                    );
                }
            }
            default -> { }
        }
    }
}
