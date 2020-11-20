import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;

import java.util.Arrays;

public class PackBot {
    public static void main(String[] args) {
        if(args.length != 1)
            throw new IllegalStateException("Args must have TOEN\nContent : "+ Arrays.toString(args));

        final String TOKEN = args[0];

        DiscordClient client = DiscordClient.create(TOKEN);
        GatewayDiscordClient gate = client.login().block();

        gate.on(MessageCreateEvent.class).subscribe(event -> {
            Message msg = event.getMessage();

            if("!ping".equals(msg.getContent())) {
                MessageChannel ch = msg.getChannel().block();

                if(ch == null)
                    return;

                ch.createMessage("Poinging!").block();
            }
        });

        gate.onDisconnect().block();
    }
}
