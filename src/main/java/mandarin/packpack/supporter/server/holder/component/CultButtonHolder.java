package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class CultButtonHolder extends ComponentHolder {
    public CultButtonHolder(Message author, Message msg, String channelID, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        registerAutoExpiration(10000);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
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
