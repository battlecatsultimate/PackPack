package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

public class ContributorRemove extends ConstraintCommand {

    public ContributorRemove(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        JDA client = ch.getJDA();

        String[] contents = loader.getContent().split(" ");

        if(contents.length < 2) {
            createMessageWithNoPings(ch, "You have to specify member ID or mention of member");
            return;
        }

        validUser(contents[1], client, () -> {
            String id = contents[1].replaceAll("<@!|<@|>", "");
            StaticStore.contributors.remove(id);
            createMessageWithNoPings(ch, "Removed <@!"+id+"> from contributor list");
        }, () -> createMessageWithNoPings(ch, "Not a valid user"));
    }

    private void validUser(String id, JDA client, Runnable found, Runnable notFound) {
        id = id.replaceAll("<!@|<@|>", "");

        client.retrieveUserById(id).queue(u -> {
            if (u != null)
                found.run();
            else
                notFound.run();
        }, e -> notFound.run());
    }
}
