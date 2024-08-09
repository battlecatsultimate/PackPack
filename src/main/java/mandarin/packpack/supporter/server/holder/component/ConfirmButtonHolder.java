package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class ConfirmButtonHolder extends ComponentHolder {
    private final Runnable action;

    /**
     * Perform {@code action} only when message deletion was confirmed
     */
    private final boolean ensureDeletion;

    public ConfirmButtonHolder(Message author, Message msg, String channelID, CommonStatic.Lang.Locale lang, Runnable action) {
        super(author, channelID, msg, lang);

        this.action = action;

        ensureDeletion = false;

        registerAutoExpiration(FIVE_MIN);
    }

    public ConfirmButtonHolder(Message author, Message msg, String channelID, CommonStatic.Lang.Locale lang, boolean ensureDeletion, Runnable action) {
        super(author, channelID, msg, lang);

        this.action = action;
        this.ensureDeletion = ensureDeletion;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "confirm" -> {
                if (ensureDeletion) {
                    message.delete().queue(unused -> action.run(), e -> {
                        StaticStore.logger.uploadErrorLog(e, "E/ConfirmButtonHolder::onEvent - Failed to delete message");

                        action.run();
                    });
                } else {
                    MessageChannelUnion channel = message.getChannel();

                    if (channel instanceof ThreadChannel tc && tc.isArchived()) {
                        message.editMessageComponents().queue();
                    } else {
                        message.delete().queue();
                    }

                    action.run();
                }
            }
            case "cancel" -> message.delete().queue();
        }

        end(false);
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
