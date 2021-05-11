package mandarin.packpack.commands.bot;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class ContributorRemove extends ConstraintCommand {

    private final GatewayDiscordClient client;

    public ContributorRemove(ROLE role, int lang, IDHolder id, GatewayDiscordClient client) {
        super(role, lang, id);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ");

        if(contents.length < 2) {
            createMessageWithNoPings(ch, "You have to specify member ID or mention of member");
            return;
        }

        if(validUser(contents[1])) {
            String id = contents[1].replaceAll("<@!|<@|>", "");
            StaticStore.contributors.remove(id);
            createMessageWithNoPings(ch, "Removed <@!"+id+"> from contributor list");
        } else {
            createMessageWithNoPings(ch, "Not a valid user");
        }
    }

    private boolean validUser(String id) {
        id = id.replaceAll("<!@|<@|>", "");

        try {
            client.getUserById(Snowflake.of(id)).block();

            return true;
        } catch (Exception e) {
            return  false;
        }
    }
}
