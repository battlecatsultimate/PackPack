package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class AnnouncementAdditionalMessageHolder extends ModalHolder {
    private final IDHolder holder;

    public AnnouncementAdditionalMessageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, channelID, message, lang);

        this.holder = holder;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals("additional")) {
            return;
        }

        String messageContent = getValueFromMap(event.getValues(), "message").strip();

        if (messageContent.isBlank()) {
            holder.announceMessage = null;
        } else {
            holder.announceMessage = messageContent;
        }

        goBack(event);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }
}
