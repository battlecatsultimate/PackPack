package mandarin.packpack.commands.bot.manage;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;

public class RegisterFixing extends ConstraintCommand {
    public RegisterFixing(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Please specify guild ID");

            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "ID `" + contents[1] + "` isn't numeric");

            return;
        }

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        Guild g = client.getGuildById(contents[1]);

        if(g != null) {
            StaticStore.needFixing.add(contents[1]);

            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Added `" + contents[1] + "` [**" + g.getName() + "**] as fixing server");
        } else {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Couldn't find such guild");
        }
    }
}
