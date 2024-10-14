package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class ConfirmPopUpHolder extends ComponentHolder {
    private final Consumer<GenericComponentInteractionCreateEvent> onConfirm;
    @Nullable
    private final Consumer<GenericComponentInteractionCreateEvent>  onCancel;

    public ConfirmPopUpHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, Message message, Consumer<GenericComponentInteractionCreateEvent> onConfirm, @Nonnull Consumer<GenericComponentInteractionCreateEvent> onCancel, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        registerAutoExpiration(FIVE_MIN);
    }

    public ConfirmPopUpHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, Message message, Consumer<GenericComponentInteractionCreateEvent> onConfirm, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.onConfirm = onConfirm;
        this.onCancel = null;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "confirm" -> {
                end(false);
                onConfirm.accept(event);
            }
            case "cancel" -> {
                end(false);

                if (onCancel == null) {
                    goBack(event);
                } else {
                    onCancel.accept(event);
                }
            }
        }
    }

    @Override
    public void onConnected(Holder parent) {

    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("ui.confirmExpired", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
