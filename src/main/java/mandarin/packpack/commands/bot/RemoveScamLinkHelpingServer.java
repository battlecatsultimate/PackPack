package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;

public class RemoveScamLinkHelpingServer extends ConstraintCommand {
    public RemoveScamLinkHelpingServer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(ch, loader.getMessage(), "Usage : p!ashs [Server ID]");

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "Server ID must be numeric!");

            return;
        }


        if(!StaticStore.scamLink.servers.contains(contents[1])) {
            replyToMessageSafely(ch, loader.getMessage(), "This server is already unregistered as helping server");

            return;
        }

        StaticStore.scamLink.servers.remove(contents[1]);
        replyToMessageSafely(ch, loader.getMessage(), "Removed server "+contents[1]);
    }
}
