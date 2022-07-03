package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FormButtonHolder extends InteractionHolder<ButtonInteractionEvent> {

    private final Message embed;
    private final ConfigHolder config;
    private final int lang;
    private final Form f;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final String channelID;
    private final String memberID;
    private final ArrayList<Integer> lv;

    public FormButtonHolder(Form f, Message author, Message msg, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, ArrayList<Integer> lv, int lang, String channelID, String memberID) {
        super(ButtonInteractionEvent.class);

        this.embed = msg;
        this.config = config;
        this.lang = lang;
        this.channelID = channelID;
        this.memberID = memberID;
        this.f = f;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.lv = lv;

        Timer autoFinsh = new Timer();

        autoFinsh.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), FormButtonHolder.this);

                expire("");
            }
        }, FIVE_MIN);
    }

    @Override
    public int handleEvent(ButtonInteractionEvent event) {
        MessageChannel ch = embed.getChannel();

        if (!ch.getId().equals(channelID)) {
            return RESULT_STILL;
        }

        if(event.getInteraction().getMember() == null)
            return RESULT_STILL;

        Member mem = event.getInteraction().getMember();

        if(!mem.getId().equals(memberID))
            return RESULT_STILL;

        Message m = event.getMessage();

        if(!m.getId().equals(embed.getId()))
            return RESULT_STILL;

        int diff = 0;

        switch (event.getComponentId()) {
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
            EntityHandler.performUnitEmb(newForm, ch, config, isFrame, talent, extra, lv, lang, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return RESULT_FINISH;
    }

    @Override
    public void performInteraction(ButtonInteractionEvent event) {
        event.getMessage().delete().queue();
    }

    @Override
    public void clean() {

    }

    @Override
    public void expire(String id) {
        ArrayList<Button> buttons = new ArrayList<>();

        for(Button button : embed.getButtons()) {
            buttons.add(button.asDisabled());
        }

        embed.editMessageComponents(ActionRow.of(buttons)).queue(null, e -> {});

        expired = true;
    }
}
