package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class AddScamLinkHelpingServer extends ConstraintCommand {
    public AddScamLinkHelpingServer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

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
