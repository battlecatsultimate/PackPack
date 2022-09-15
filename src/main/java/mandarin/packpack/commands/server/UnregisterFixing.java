package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class UnregisterFixing extends ConstraintCommand {
    public UnregisterFixing(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.sendMessage("Please specify guild ID").queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            ch.sendMessage("ID `"+contents[1]+"` isn't numeric").queue();

            return;
        }

        if(!StaticStore.needFixing.contains(contents[1])) {
            ch.sendMessage("This server (`"+contents[1]+"`) isn't registered as fixing server already").queue();

            return;
        }

        StaticStore.needFixing.remove(contents[1]);

        ch.sendMessage("Removed `"+contents[1]+"` from fixing server list").queue();
    }
}
