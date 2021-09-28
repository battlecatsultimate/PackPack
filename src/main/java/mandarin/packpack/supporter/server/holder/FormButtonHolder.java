package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.component.MessageComponent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.Command;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FormButtonHolder extends InteractionHolder<ButtonInteractionEvent> {

    private final Message embed;
    private final int lang;
    private final Form f;

    private final boolean isFrame;
    private final boolean talent;
    private final String channelID;
    private final String memberID;
    private final int[] lv;

    public FormButtonHolder(Form f, Message author, Message msg, boolean isFrame, boolean talent, int[] lv, int lang, String channelID, String memberID) {
        super(ButtonInteractionEvent.class);

        this.embed = msg;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;
        this.f = f;

        this.isFrame = isFrame;
        this.talent = talent;
        this.lv = lv;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                author.getAuthor().ifPresent(u -> StaticStore.removeHolder(u.getId().asString(), FormButtonHolder.this));

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        MessageChannel ch = embed.getChannel().block();

        if(ch == null)
            return RESULT_STILL;

        if (!ch.getId().asString().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember().isEmpty())
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember().get();

        if(!mem.getId().asString().equals(memberID))
            return RESULT_STILL;

        if(event.getMessage().isEmpty())
            return RESULT_STILL;

        Message m = event.getMessage().get();

        if(!m.getId().asString().equals(embed.getId().asString()))
            return RESULT_STILL;

        int diff = 0;

        switch (event.getCustomId()) {
            case "first":
                diff = -2;
                break;
            case "pre":
                diff = -1;
                break;
            case "next":
                diff = 1;
                break;
            case "final":
                diff = 2;
                break;
        }

        if(diff == 0) {
            return RESULT_STILL;
        }

        if(f.fid + diff < 0)
            return RESULT_STILL;

        if(f.unit == null)
            return RESULT_STILL;

        Form newForm = f.unit.forms[f.fid + diff];

        try {
            EntityHandler.showUnitEmb(newForm, ch, isFrame, talent, lv, lang, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RESULT_FINISH;
    }

    @Override
    public Mono<?> getInteraction(ButtonInteractionEvent event) {
        return event.deferEdit().then(event.getInteractionResponse().deleteInitialResponse());
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        Command.editMessage(embed, m -> {
            ArrayList<Button> buttons = new ArrayList<>();

            for(LayoutComponent layoutComponent : embed.getComponents()) {
                for(MessageComponent component : layoutComponent.getChildren()) {
                    if(component instanceof Button) {
                        if(((Button) component).getStyle() == Button.Style.SECONDARY) {
                            buttons.add(((Button) component).disabled(true));
                        } else {
                            buttons.add((Button) component);
                        }
                    }
                }
            }

            m.addComponent(ActionRow.of(buttons));
        });

        expired = true;
    }
}
