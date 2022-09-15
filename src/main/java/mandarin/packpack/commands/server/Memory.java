package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class Memory extends ConstraintCommand {
    public Memory(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        long f = Runtime.getRuntime().freeMemory();
        long t = Runtime.getRuntime().totalMemory();
        long m = Runtime.getRuntime().maxMemory();
        double per = 100.0 * (t - f) / m;

        ch.sendMessage("Memory used: " + (t - f >> 20) + " MB / " + (m >> 20) + " MB, " + (int) per + "%").queue();
    }
}
