package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class RegisterFixing extends ConstraintCommand {
    public RegisterFixing(ROLE role, int lang, IDHolder id) {
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

        JDA client = event.getJDA();

        Guild g = client.getGuildById(contents[1]);

        if(g != null) {
            StaticStore.needFixing.add(contents[1]);

            ch.sendMessage("Added `"+contents[1]+"` [**"+g.getName()+"**] as fixing server").queue();
        } else {
            ch.sendMessage("Couldn't find such guild").queue();
        }
    }
}
