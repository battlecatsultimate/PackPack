package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.event.StageSchedule;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.concurrent.TimeUnit;

public class Test extends SingleContraintCommand {

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getMessage(event).replace("    ", "\t").split(" ");

        if(list.length >= 2) {
            StageSchedule schedule = new StageSchedule(list[1]);

            ch.createMessage(schedule.dataToString()).subscribe();
        }
    }

    @Override
    protected void setOptionalID(MessageCreateEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}
