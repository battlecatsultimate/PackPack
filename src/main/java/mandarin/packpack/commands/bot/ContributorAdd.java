package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import javax.annotation.Nonnull;

public class ContributorAdd extends ConstraintCommand {

    public ContributorAdd(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        ShardManager client = ch.getJDA().getShardManager();

        if (client == null)
            return;

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            createMessageWithNoPings(ch, "You have to specify member ID or mention of member");
            return;
        }

        validUser(contents[1], client, () -> {
            String id = contents[1].replaceAll("<@!|<@|>", "");

            if(!StaticStore.contributors.contains(id))
                StaticStore.contributors.add(id);

            createMessageWithNoPings(ch, "Added <@!"+id+"> as contributor");
        }, () -> createMessageWithNoPings(ch, "Not a valid user"));
    }

    private void validUser(String id, ShardManager client, Runnable found, Runnable notFound) {
        id = id.replaceAll("<@!|<@|>", "");

        client.retrieveUserById(id).queue(u -> {
            if (u != null)
                found.run();
            else
                notFound.run();
        }, e -> notFound.run());
    }
}
