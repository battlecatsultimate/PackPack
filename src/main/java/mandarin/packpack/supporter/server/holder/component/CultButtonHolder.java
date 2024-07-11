package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class CultButtonHolder extends ComponentHolder {
    public CultButtonHolder(Message author, Message msg, String channelID, String memberID, CommonStatic.Lang.Locale lang) {
        super(author, channelID, msg, lang);

        StaticStore.executorHandler.postDelayed(10000, () -> {
            if(expired)
                return;

            expired = true;

            msg.editMessage(LangID.getStringByID("hi_sp_0_2", lang))
                    .setComponents()
                    .mentionRepliedUser(false)
                    .queue();

            StaticStore.removeHolder(memberID, CultButtonHolder.this);
        });
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "yes" -> {
                StaticStore.cultist.add(userID);

                message.editMessage(LangID.getStringByID("hi_sp_0_0", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;

                StaticStore.removeHolder(userID, this);
            }
            case "no" -> {
                message.editMessage(LangID.getStringByID("hi_sp_0_1", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;

                StaticStore.removeHolder(userID, this);
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        expired = true;

        message.editMessage(LangID.getStringByID("hi_sp_0_2", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
