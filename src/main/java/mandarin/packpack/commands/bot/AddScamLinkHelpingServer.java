package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class AddScamLinkHelpingServer extends ConstraintCommand {
    public AddScamLinkHelpingServer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            ch.sendMessage("Usage : p!ashs [Server ID]").queue();

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            ch.sendMessage("Server ID must be numeric!").queue();

            return;
        }


        if(StaticStore.scamLink.servers.contains(contents[1])) {
            ch.sendMessage("This server is already registered as helping server").queue();

            return;
        }

        StaticStore.scamLink.servers.add(contents[1]);
        ch.sendMessage("Added server "+contents[1]).queue();
    }
}
