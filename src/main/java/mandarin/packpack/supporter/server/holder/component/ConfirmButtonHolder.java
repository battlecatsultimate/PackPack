package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class ConfirmButtonHolder extends ComponentHolder {
    private final Runnable action;
    private final int lang;

    public ConfirmButtonHolder(Message author, Message msg, String channelID, int lang, Runnable action) {
        super(author, channelID, msg);

        this.action = action;
        this.lang = lang;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), ConfirmButtonHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(userID, this);

        switch (event.getComponentId()) {
            case "confirm" -> {
                message.delete().queue();
                action.run();
            }
            case "cancel" -> message.delete().queue();
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        message.editMessage(LangID.getStringByID("confirm_expired", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
