package mandarin.packpack.commands;

import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, int lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1), true);
    }

    @Override
    protected void doThing(GenericMessageEvent event) {
        MessageChannel ch = getChannel(event);
        Guild g = getGuild(event);

        if(ch == null || g == null)
            return;

        String[] contents = getContent(event).split(" ", 2);

        if(contents.length < 2)
            return;

        g.modifyNickname(g.getSelfMember(), contents[1]).queue();
    }

    @Override
    protected void setOptionalID(GenericMessageEvent event) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}