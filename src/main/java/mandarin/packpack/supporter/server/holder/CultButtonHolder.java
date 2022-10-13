package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Timer;
import java.util.TimerTask;

public class CultButtonHolder extends InteractionHolder<ButtonInteractionEvent> {
    private final Message msg;
    private final String channelID;
    private final String memberID;

    private final int lang;

    public CultButtonHolder(Message author, Message msg, String channelID, String memberID, int lang) {
        super(ButtonInteractionEvent.class, author);

        this.msg = msg;
        this.channelID = channelID;
        this.memberID = memberID;

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
                        .queue();

                StaticStore.removeHolder(memberID, CultButtonHolder.this);
            }
        }, 10000);
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        MessageChannel ch = msg.getChannel();

        if (!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember();

        if(!mem.getId().equals(memberID))
            return RESULT_STILL;

        Message m = event.getMessage();

        if(!m.getId().equals(msg.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(ButtonInteractionEvent event) {
        switch (event.getComponentId()) {
            case "yes":
                StaticStore.cultist.add(memberID);

                msg.editMessage(LangID.getStringByID("hi_sp_0_0", lang))
                        .setComponents()
                        .queue();

                expired = true;

                StaticStore.removeHolder(memberID, this);

                break;
            case "no":
                msg.editMessage(LangID.getStringByID("hi_sp_0_1", lang))
                        .setComponents()
                        .queue();

                expired = true;

                StaticStore.removeHolder(memberID, this);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("hi_sp_0_2", lang))
                .setComponents()
                .queue();
    }
}
