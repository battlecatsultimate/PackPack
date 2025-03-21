package mandarin.packpack.commands.bot.manage;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class UnregisterFixing extends ConstraintCommand {
    public UnregisterFixing(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, true);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
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

        if(!StaticStore.needFixing.contains(contents[1])) {
            ch.sendMessage("This server (`"+contents[1]+"`) isn't registered as fixing server already").queue();

            return;
        }

        StaticStore.needFixing.remove(contents[1]);

        ch.sendMessage("Removed `"+contents[1]+"` from fixing server list").queue();
    }
}
