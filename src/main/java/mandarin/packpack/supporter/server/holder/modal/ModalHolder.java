package mandarin.packpack.supporter.server.holder.modal;

import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class ModalHolder extends Holder {

    public ModalHolder(@Nonnull Message author, @Nonnull String channelID, @Nonnull String messageID) {
        super(author, channelID, messageID);
    }

    @Override
    public final STATUS handleEvent(Event event) {
        if(event instanceof ModalInteractionEvent modalEvent && canHandleEvent(modalEvent)) {
            onEvent(modalEvent);
        }

        return STATUS.WAIT;
    }

    public abstract void onEvent(ModalInteractionEvent event);

    @Nonnull
    public String getValueFromMap(List<ModalMapping> mappings, String key) {
        for(int i = 0; i < mappings.size(); i++) {
            ModalMapping mapping = mappings.get(i);

            if(mapping.getId().equals(key)) {
                return mapping.getAsString();
            }
        }

        StringBuilder builder = new StringBuilder("E/ModalHolder::getValueFromMap - No such key ")
                .append(key)
                .append(" found in this mappings\n\nMapping : [\n");

        for(int i = 0; i < mappings.size(); i++) {
            builder.append(mappings.get(i).getId())
                    .append(" [")
                    .append(mappings.get(i).getType())
                    .append("] -> ")
                    .append(mappings.get(i).getAsString())
                    .append("\n");
        }

        throw new IllegalStateException(builder.toString());
    }

    private boolean canHandleEvent(ModalInteractionEvent event) {
        boolean result = event.getChannel().getId().equals(channelID);

        result &= event.getMessage() == null || event.getMessage().getId().equals(messageID);

        result &= event.getUser().getId().equals(userID);

        return result;
    }
}
