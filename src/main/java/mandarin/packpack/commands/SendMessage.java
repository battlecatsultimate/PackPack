package mandarin.packpack.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import mandarin.packpack.supporter.server.data.IDHolder;

public class SendMessage extends ConstraintCommand {
    private final GatewayDiscordClient client;

    public SendMessage(ROLE role, int lang, IDHolder id, GatewayDiscordClient client) {
        super(role, lang, id);

        this.client = client;
    }

    @Override
    public void doSomething(MessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        String[] contents = getContent(event).split(" ", 4);

        if(contents.length != 4) {
            createMessage(ch, m -> m.content("`p!sm [Guild ID] [Channel ID] [Contents]`"));
            return;
        }

        client.getGuildById(Snowflake.of(contents[1])).subscribe(g -> g.getChannelById(Snowflake.of(contents[2])).subscribe(c -> {
            if(c instanceof MessageChannel) {
                String co;

                if(contents[3].startsWith("`"))
                    co = contents[3].substring(1, contents[3].length() - 1).replace("\\e", "");
                else
                    co = contents[3].replace("\\e", "");

                createMessage((MessageChannel) c, m -> m.content(co));

                createMessage(ch, m -> m.content("Sent message : "+co));

                System.out.println(co);
            } else {
                createMessage(ch, m -> m.content("Channel isn't message channel"));
            }
        }, e -> createMessage(ch, m -> m.content("No such channel found"))), e -> createMessage(ch, m -> m.content("No such guild found")));
    }
}
