package mandarin.packpack.supporter.server.holder;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ConfirmButtonHolder extends InteractionHolder<ButtonInteractionEvent> {
    private final Runnable action;
    private final int lang;
    private final String channelID;
    private final String memberID;

    private final Message msg;

    public ConfirmButtonHolder(Message msg, Message author, String channelID, String memberID, Runnable action, int lang) {
        super(ButtonInteractionEvent.class);
        this.action = action;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;

        this.msg = msg;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), ConfirmButtonHolder.this));

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        MessageChannel ch = msg.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if(!ch.getId().asString().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member m = event.getInteraction().getMember().get();

        if(!m.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message me = event.getMessage().get();

        if(!me.getId().asString().equals(msg.getId().asString()))
            return RESULT_STILL;

        return RESULT_FINISH;
    }

    @Override
    public Mono<?> getInteraction(ButtonInteractionEvent event) {
        expired = true;

        StaticStore.removeHolder(memberID, this);

        switch (event.getCustomId()) {
            case "confirm":
                action.run();
            case "cancel":
                return event.deferEdit().then(event.getInteractionResponse().deleteInitialResponse());
        }

        return Mono.empty();
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        expired = true;

        Command.editMessage(msg, m -> {
            ArrayList<Button> buttons = new ArrayList<>();

            m.addComponent(ActionRow.of(buttons));
            m.content(wrap(LangID.getStringByID("confirm_expire", lang)));
        });
    }
}
