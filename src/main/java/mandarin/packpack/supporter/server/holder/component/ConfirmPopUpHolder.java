package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ConfirmPopUpHolder extends ComponentHolder {
    private final Consumer<GenericComponentInteractionCreateEvent> onConfirm;
    @Nullable
    private final Consumer<GenericComponentInteractionCreateEvent>  onCancel;

    private final int lang;

    private final Message msg;

    public ConfirmPopUpHolder(Message author, Message msg, String channelID, Consumer<GenericComponentInteractionCreateEvent> onConfirm, Consumer<GenericComponentInteractionCreateEvent> onCancel, int lang) {
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

    public ConfirmPopUpHolder(Message author, Message msg, String channelID, Consumer<GenericComponentInteractionCreateEvent> onConfirm, int lang) {
        super(author, channelID, msg);

        this.onConfirm = onConfirm;
        this.onCancel = null;

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
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(userID, this);

        switch (event.getComponentId()) {
            case "confirm" -> onConfirm.accept(event);
            case "cancel" -> {
                if (onCancel == null) {
                    goBack();
                } else {
                    onCancel.accept(event);
                }
            }
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
