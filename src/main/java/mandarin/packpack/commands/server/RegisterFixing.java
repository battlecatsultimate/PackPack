package mandarin.packpack.commands.server;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.discordjson.json.GuildUpdateData;
import discord4j.rest.entity.RestGuild;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.data.IDHolder;

public class RegisterFixing extends ConstraintCommand {
    private final DiscordClient client;

    public RegisterFixing(ROLE role, int lang, IDHolder id, DiscordClient client) {
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
            createMessage(ch, m -> m.content("Please specify guild ID"));
            return;
        }

        if(!StaticStore.isNumeric(contents[1])) {
            createMessage(ch, m -> m.content("ID `"+contents[1]+"` isn't numeric"));
            return;
        }

        try {
            RestGuild rg = client.getGuildById(Snowflake.of(contents[1]));
            GuildUpdateData gud = rg.getData().block();

            StaticStore.needFixing.add(contents[1]);

            if(gud != null) {
                createMessage(ch, m -> m.content("Added `"+contents[1]+"` [**"+gud.name()+"**] as fixing server"));
            } else {
                createMessage(ch, m -> m.content("Added `"+contents[1]+"` as fixing server"));
            }
        } catch (Exception e) {
            createMessage(ch, m -> m.content("Couldn't find such guild"));
        }
    }
}
