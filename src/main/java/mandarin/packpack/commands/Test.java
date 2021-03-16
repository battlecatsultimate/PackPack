package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.event.EventFactor;
import mandarin.packpack.supporter.event.StageSchedule;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.IDHolder;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {

    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void doThing(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        File f = new File("./data/sale.tsv");

        StaticStore.event.updateStage(f, EventFactor.GLOBAL, false);

        String result = StaticStore.event.printStageEvent(EventFactor.GLOBAL);

        System.out.println(result.length());

        if(!result.isBlank() && result.length() < 2000) {
            ch.createMessage(result).subscribe();
        } else {
            System.out.println(result);
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
