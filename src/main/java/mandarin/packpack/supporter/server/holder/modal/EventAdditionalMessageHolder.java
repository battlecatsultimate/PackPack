package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.data.EventDataConfigHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EventAdditionalMessageHolder extends ModalHolder {
    private final EventDataConfigHolder holder;
    private final boolean forEventData;

    public EventAdditionalMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, EventDataConfigHolder holder, CommonStatic.Lang.Locale lang, boolean forEventData) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
        this.forEventData = forEventData;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("additional")) {
            return;
        }

        String message = getValueFromMap(event.getValues(), "message").strip();

        if (message.isBlank()) {
            if (forEventData) {
                holder.eventMessage = "";
            } else {
                holder.newVersionMessage = "";
            }
        } else {
            if (forEventData) {
                holder.eventMessage = message;
            } else {
                holder.newVersionMessage = message;
            }
        }

        goBack(event);
    }

    @Override
    public void clean() {

    }
}
