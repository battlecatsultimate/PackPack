package mandarin.packpack;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.request.RouterOptions;
import mandarin.packpack.commands.BCUStat;
import mandarin.packpack.commands.CheckBCU;
import mandarin.packpack.supporter.StaticStore;

public class PackBot {
    public static void main(String[] args) {
        final String TOKEN = args[0];

        DiscordClientBuilder<DiscordClient, RouterOptions> builder = DiscordClientBuilder.create(TOKEN);

        DiscordClient client = builder.build();

        GatewayDiscordClient gate = client.gateway().login().block();

        if(gate == null) {
            return;
        }

        gate.updatePresence(Presence.online(Activity.playing("Under Construction!"))).subscribe();

        gate.on(MessageCreateEvent.class)
                .filter(event -> {
                    MessageChannel mc = event.getMessage().getChannel().block();

                    if(mc == null)
                        return false;
                    else
                        return mc.getId().asString().equals(StaticStore.BOT_COMMANDS);
                }).subscribe(event -> {
                    Message msg = event.getMessage();

                    MessageChannel ch = msg.getChannel().block();

                    if(ch != null) {
                        switch (msg.getContent()) {
                            case "p!checkbcu":
                                new CheckBCU().execute(event);
                                break;
                            case "p!bcustat":
                                new BCUStat().execute(event);
                                break;
                        }
                    }
        });

        gate.onDisconnect().block();
    }

    private static String getNumberExtension(int n) {
        switch (n%10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }
}
