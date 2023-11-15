package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

public class ConfirmButtonHolder extends ComponentHolder {
    private final Runnable action;
    private final int lang;

    private final Message msg;

    public ConfirmButtonHolder(Message author, Message msg, String channelID, Runnable action, int lang) {
        super(author, channelID, msg.getId());

        this.action = action;
        this.lang = lang;

        this.msg = msg;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), ConfirmButtonHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        expired = true;

        StaticStore.removeHolder(userID, this);

        switch (event.getComponentId()) {
            case "confirm" -> {
                msg.delete().queue();
                action.run();
            }
            case "cancel" -> msg.delete().queue();
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
