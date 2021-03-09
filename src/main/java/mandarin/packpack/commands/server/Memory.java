package mandarin.packpack.commands.server;

import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.server.IDHolder;

public class Memory extends ConstraintCommand {
    public Memory(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        long f = Runtime.getRuntime().freeMemory();
        long t = Runtime.getRuntime().totalMemory();
        long m = Runtime.getRuntime().maxMemory();
        double per = 100.0 * (t - f) / m;

        ch.createMessage("Memory used: " + (t - f >> 20) + " MB / " + (m >> 20) + " MB, " + (int) per + "%").subscribe();
    }
}
