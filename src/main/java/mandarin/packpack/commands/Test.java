package mandarin.packpack.commands;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import mandarin.packpack.supporter.server.IDHolder;

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

        ch.createMessage(m -> m.setContent(":sunrise_over_mountains: ")).subscribe(m -> m.addReaction(ReactionEmoji.unicode(new String(Character.toChars(0x1f304)))).subscribe());
    }

    @Override
    protected void setOptionalID(MessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}
