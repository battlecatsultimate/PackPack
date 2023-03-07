package mandarin.packpack.commands.math;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class RTheta extends TimedConstraintCommand {
    public RTheta(ConstraintCommand.ROLE role, int lang, @Nullable IDHolder idHolder, long time) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_RTHETA_ID, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);
    }
}
