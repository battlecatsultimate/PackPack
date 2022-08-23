package mandarin.packpack.supporter.server.holder;

import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.util.Timer;
import java.util.TimerTask;

public class ConfirmButtonHolder extends InteractionHolder<ButtonInteractionEvent> {
    private final Runnable action;
    private final int lang;
    private final String channelID;
    private final String memberID;

    private final Message msg;

    public ConfirmButtonHolder(Message msg, Message author, String channelID, Runnable action, int lang) {
        super(ButtonInteractionEvent.class, author);
        this.action = action;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = author.getAuthor().getId();

        this.msg = msg;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), ConfirmButtonHolder.this);

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        MessageChannel ch = msg.getChannel();

        if(!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member m = event.getInteraction().getMember();

        if(!m.getId().equals(memberID))
            return RESULT_STILL;

        Message me = event.getMessage();

        if(!me.getId().equals(msg.getId()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(ButtonInteractionEvent event) {
        expired = true;

        StaticStore.removeHolder(memberID, this);

        switch (event.getComponentId()) {
            case "confirm":
                event.getMessage().delete().queue();
                action.run();

                break;
            case "cnacle":
                event.getMessage().delete().queue();
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        msg.editMessage(LangID.getStringByID("confirm_expired", lang))
                .setActionRows()
                .queue();
    }
}
