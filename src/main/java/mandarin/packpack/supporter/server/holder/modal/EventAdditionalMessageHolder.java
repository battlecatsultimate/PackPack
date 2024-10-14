package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EventAdditionalMessageHolder extends ModalHolder {
    private final IDHolder holder;
    private final CommonStatic.Lang.Locale locale;

    public EventAdditionalMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, IDHolder holder, CommonStatic.Lang.Locale lang, CommonStatic.Lang.Locale locale) {
        super(author, userID, channelID, message, lang);

        this.holder = holder;
        this.locale = locale;
    }

    @Override
    public void onEvent(@Nonnull ModalInteractionEvent event) {
        if (!event.getModalId().equals("additional")) {
            return;
        }

        String message = getValueFromMap(event.getValues(), "message").strip();

        if (message.isBlank()) {
            holder.eventMessage.remove(locale);
        } else {
            holder.eventMessage.put(locale, message);
        }

        goBack(event);
    }

    @Override
    public void clean() {

    }
}
