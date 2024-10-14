package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.util.concurrent.TimeUnit;

public class Test extends GlobalTimedConstraintCommand {
    public Test(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, String mainID) {
        super(role, lang, id, mainID, TimeUnit.SECONDS.toMillis(1), false);
    }

    @Override
    protected void doThing(CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ch.sendMessage("Test Message")
                .setMessageReference((Message) null)
                .queue();
    }

    @Override
    protected void setOptionalID(CommandLoader loader) {
        optionalID = "";
    }

    @Override
    protected void prepareAborts() {

    }
}