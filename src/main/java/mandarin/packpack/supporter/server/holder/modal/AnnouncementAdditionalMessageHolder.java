package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnnouncementAdditionalMessageHolder extends ModalHolder {
    private final IDHolder holder;

    public AnnouncementAdditionalMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, IDHolder holder, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("additional")) {
            return;
        }

        String messageContent = getValueFromMap(event.getValues(), "message").strip();

        if (messageContent.isBlank()) {
            holder.announceMessage = "";
        } else {
            holder.announceMessage = messageContent;
        }

        goBack(event);
    }

    @Override
    public void clean() {

    }
}
