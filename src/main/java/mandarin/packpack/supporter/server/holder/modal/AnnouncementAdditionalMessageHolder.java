package mandarin.packpack.supporter.server.holder.modal;

import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class AnnouncementAdditionalMessageHolder extends ModalHolder {
    private final IDHolder holder;

    public AnnouncementAdditionalMessageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, IDHolder holder) {
        super(author, channelID, message);

        this.holder = holder;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("additional")) {
            return;
        }

        holder.announceMessage = getValueFromMap(event.getValues(), "message").strip();

        goBack(event);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }
}
