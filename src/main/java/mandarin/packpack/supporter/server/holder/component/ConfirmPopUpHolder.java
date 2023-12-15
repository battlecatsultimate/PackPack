package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.function.Function;

public class ConfirmPopUpHolder extends ComponentHolder {
    private final Function<GenericComponentInteractionCreateEvent, Void> onConfirm;
    private final Function<GenericComponentInteractionCreateEvent, Void>  onCancel;

    private final int lang;

    private final Message msg;

    public ConfirmPopUpHolder(Message author, Message msg, String channelID, Function<GenericComponentInteractionCreateEvent, Void> onConfirm, Function<GenericComponentInteractionCreateEvent, Void> onCancel, int lang) {
        super(author, channelID, msg.getId());

        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        this.lang = lang;

        this.msg = msg;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), ConfirmPopUpHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(userID, this);

        switch (event.getComponentId()) {
            case "confirm" -> onConfirm.apply(event);
            case "cancel" -> onCancel.apply(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("confirm_expired", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
