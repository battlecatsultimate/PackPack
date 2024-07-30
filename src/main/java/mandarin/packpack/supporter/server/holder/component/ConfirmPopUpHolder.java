package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ConfirmPopUpHolder extends ComponentHolder {
    private final Consumer<GenericComponentInteractionCreateEvent> onConfirm;
    @Nullable
    private final Consumer<GenericComponentInteractionCreateEvent>  onCancel;

    public ConfirmPopUpHolder(Message author, String channelID, Message msg, Consumer<GenericComponentInteractionCreateEvent> onConfirm, @NotNull Consumer<GenericComponentInteractionCreateEvent> onCancel, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        this.onConfirm = onConfirm;
        this.onCancel = onCancel;

        registerAutoExpiration(FIVE_MIN);
    }

    public ConfirmPopUpHolder(Message author, String channelID, Message msg, Consumer<GenericComponentInteractionCreateEvent> onConfirm, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        this.onConfirm = onConfirm;
        this.onCancel = null;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "confirm" -> onConfirm.accept(event);
            case "cancel" -> {
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
