package mandarin.packpack.commands.server;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class RegisterFixing extends ConstraintCommand {
    public RegisterFixing(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            ch.sendMessage("Please specify guild ID").queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            ch.sendMessage("ID `"+contents[1]+"` isn't numeric").queue();

            return;
        }

        JDA client = ch.getJDA();

        Guild g = client.getGuildById(contents[1]);

        if(g != null) {
            StaticStore.needFixing.add(contents[1]);

            ch.sendMessage("Added `"+contents[1]+"` [**"+g.getName()+"**] as fixing server").queue();
        } else {
            ch.sendMessage("Couldn't find such guild").queue();
        }
    }
}
