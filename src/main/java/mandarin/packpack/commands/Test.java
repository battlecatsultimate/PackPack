package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.event.StageSchedule;
import mandarin.packpack.supporter.server.data.IDHolder;

import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(MessageEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getContent(event).replace("    ", "\t").split(" ", 2);

        if(list.length >= 2) {
            StageSchedule gacha = new StageSchedule(list[1]);

            ch.createMessage(gacha.beautify()).subscribe();
        }
    }

    @Override
    protected void setOptionalID(MessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}