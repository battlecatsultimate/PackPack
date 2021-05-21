package mandarin.packpack;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.rest.request.RouterOptions;
import mandarin.packpack.supporter.server.slash.SlashBuilder;

public class SlashCommandUpdater {
    public static void main(String[] args) {
        final String TOKEN = args[0];

        DiscordClientBuilder<DiscordClient, RouterOptions> builder = DiscordClientBuilder.create(TOKEN);

        DiscordClient client = builder.build();

        GatewayDiscordClient gate = client.gateway().login().block();

        if(gate == null) {
            System.out.println("Gate is null");
            return;
        }

        SlashBuilder.build(gate);
    }
}
