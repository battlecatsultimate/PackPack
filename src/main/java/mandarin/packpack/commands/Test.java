package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.event.GachaSchedule;
import mandarin.packpack.supporter.event.ItemSchedule;
import mandarin.packpack.supporter.server.IDHolder;

import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(MessageCreateEvent event) {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] list = getMessage(event).replace("    ", "\t").split(" ", 2);

        if(list.length >= 2) {
            GachaSchedule gacha = new GachaSchedule(list[1]);

            ch.createMessage(gacha.dataToString()).subscribe();
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
