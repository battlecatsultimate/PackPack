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
            replyToMessageSafely(ch, loader.getMessage(),"Please specify guild ID");

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "ID `" + contents[1] + "` isn't numeric");

            return;
        }

        if(!StaticStore.needFixing.contains(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "This server (`" + contents[1] + "`) isn't registered as fixing server already");

            return;
        }

        StaticStore.needFixing.remove(contents[1]);

        replyToMessageSafely(ch, loader.getMessage(), "Removed `" + contents[1] + "` from fixing server list");
    }
}
