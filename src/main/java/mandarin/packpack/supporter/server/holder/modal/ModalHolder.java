package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class ModalHolder extends Holder {
    public ModalHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);
    }

    @Override
    public final STATUS handleEvent(@Nonnull Event event) {
        if(event instanceof ModalInteractionEvent modalEvent && canHandleEvent(modalEvent)) {
            onEvent(modalEvent);

            return STATUS.FINISH;
        }

        return STATUS.WAIT;
    }

    public abstract void onEvent(@Nonnull ModalInteractionEvent event);

    @Nonnull
    public String getValueFromMap(List<ModalMapping> mappings, String key) {
        for(int i = 0; i < mappings.size(); i++) {
            ModalMapping mapping = mappings.get(i);

            if(mapping.getCustomId().equals(key)) {
                return mapping.getAsString();
            }
        }

        StringBuilder builder = new StringBuilder("E/ModalHolder::getValueFromMap - No such key ")
                .append(key)
                .append(" found in this mappings\n\nMapping : [\n");

        for(int i = 0; i < mappings.size(); i++) {
            builder.append(mappings.get(i).getCustomId())
                    .append(" [")
                    .append(mappings.get(i).getType())
                    .append("] -> ")
                    .append(mappings.get(i).getAsString())
                    .append("\n");
        }

        throw new IllegalStateException(builder.toString());
    }

    @Override
    public final Type getType() {
        return Type.MODAL;
    }

    @Override
    public final void onConnected(Holder parent) {

    }

    @Override
    public final void onConnected(@Nonnull IMessageEditCallback event, @Nonnull Holder parent) {

    }

    @Override
    public final void onExpire() {

    }

    private boolean canHandleEvent(ModalInteractionEvent event) {
        boolean result = event.getChannel().getId().equals(channelID);

        result &= event.getMessage() == null || event.getMessage().getId().equals(message.getId());

        result &= event.getUser().getId().equals(userID);

        return result;
    }
}
