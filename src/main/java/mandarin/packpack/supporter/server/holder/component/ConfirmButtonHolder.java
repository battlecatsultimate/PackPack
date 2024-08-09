package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class ConfirmButtonHolder extends ComponentHolder {
    private final Runnable action;

    public ConfirmButtonHolder(Message author, Message msg, String channelID, CommonStatic.Lang.Locale lang, Runnable action) {
        super(author, channelID, msg, lang);

        this.action = action;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "confirm" -> {
                MessageChannelUnion channel = message.getChannel();

                if (channel instanceof ThreadChannel tc && tc.isArchived()) {
                    message.editMessageComponents().queue();
                } else {
                    message.delete().queue();
                }

                action.run();
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
