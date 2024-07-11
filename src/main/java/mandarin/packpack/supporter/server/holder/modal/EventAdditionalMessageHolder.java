package mandarin.packpack.supporter.server.holder.modal;

import common.CommonStatic;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.jetbrains.annotations.NotNull;

public class EventAdditionalMessageHolder extends ModalHolder {
    private final IDHolder holder;
    private final CommonStatic.Lang.Locale locale;

    public EventAdditionalMessageHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message, IDHolder holder, CommonStatic.Lang.Locale lang, CommonStatic.Lang.Locale locale) {
        super(author, channelID, message, lang);

        this.holder = holder;
        this.locale = locale;
    }

    @Override
    public void onEvent(@NotNull ModalInteractionEvent event) {
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

    @Override
    public void onExpire(String id) {

    }
}
