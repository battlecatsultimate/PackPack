package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CultButtonHolder extends ComponentHolder {
    public CultButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        registerAutoExpiration(10000);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "yes" -> {
                StaticStore.cultist.add(userID);

                message.editMessage(LangID.getStringByID("hi.special.accept", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
            case "no" -> {
                message.editMessage(LangID.getStringByID("hi.special.denial", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                end(true);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        message.editMessage(LangID.getStringByID("hi.special.expired", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
