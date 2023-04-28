package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class ContributorRemove extends ConstraintCommand {

    public ContributorRemove(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        JDA client = ch.getJDA();

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessageWithNoPings(ch, "You have to specify member ID or mention of member");
            return;
        }

        if(validUser(contents[1], client)) {
            String id = contents[1].replaceAll("<@!|<@|>", "");
            StaticStore.contributors.remove(id);
            createMessageWithNoPings(ch, "Removed <@!"+id+"> from contributor list");
        } else {
            createMessageWithNoPings(ch, "Not a valid user");
        }
    }

    private boolean validUser(String id, JDA client) {
        id = id.replaceAll("<!@|<@|>", "");

        User u = client.retrieveUserById(id).complete();

        return u != null;
    }
}
