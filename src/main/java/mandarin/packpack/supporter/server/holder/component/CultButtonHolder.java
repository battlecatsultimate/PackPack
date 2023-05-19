package mandarin.packpack.supporter.server.holder.component;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.Timer;
import java.util.TimerTask;

public class CultButtonHolder extends ComponentHolder {
    private final Message msg;

    private final int lang;

    public CultButtonHolder(Message author, Message msg, String channelID, String memberID, int lang) {
        super(author, channelID, msg.getId());

        this.msg = msg;

        this.lang = lang;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                msg.editMessage(LangID.getStringByID("hi_sp_0_2", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                StaticStore.removeHolder(memberID, CultButtonHolder.this);
            }
        }, 10000);
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "yes" -> {
                StaticStore.cultist.add(userID);

                msg.editMessage(LangID.getStringByID("hi_sp_0_0", lang))
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue();

                expired = true;

                StaticStore.removeHolder(userID, this);
            }
            case "no" -> {
                msg.editMessage(LangID.getStringByID("hi_sp_0_1", lang))
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

        msg.editMessage(LangID.getStringByID("hi_sp_0_2", lang))
                .setComponents()
                .mentionRepliedUser(false)
                .queue();
    }
}
