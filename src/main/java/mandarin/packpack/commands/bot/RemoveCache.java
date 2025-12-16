package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class RemoveCache extends ConstraintCommand {
    public RemoveCache(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ",  2);

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), "Format `p!rc [Cache Name]`");

            return;
        }

        if(StaticStore.imgur.removeCache(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "Successfully removed cache : "+contents[1]);
        } else {
            replyToMessageSafely(ch, loader.getMessage(), "No such cache found : "+contents[1]);
        }
    }
}
