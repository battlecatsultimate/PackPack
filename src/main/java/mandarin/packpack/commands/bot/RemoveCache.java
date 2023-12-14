package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;

public class RemoveCache extends ConstraintCommand {
    public RemoveCache(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ",  2);

        if(contents.length < 2) {
            ch.sendMessage("Format `p!rc [Cache Name]`").queue();

            return;
        }

        if(StaticStore.imgur.removeCache(contents[1])) {
            ch.sendMessage("Successfully removed cache : "+contents[1]).queue();
        } else {
            ch.sendMessage("No such cache found : "+contents[1]).queue();
        }
    }
}
