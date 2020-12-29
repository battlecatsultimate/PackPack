package mandarin.packpack;

import common.CommonStatic;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.request.RouterOptions;
import mandarin.packpack.commands.*;
import mandarin.packpack.supporter.PackContext;
import mandarin.packpack.supporter.StaticStore;

import java.util.concurrent.atomic.AtomicReference;

public class PackBot {
    public static void main(String[] args) {
        initialize();

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
                    else {
                        AtomicReference<Boolean> mandarin = new AtomicReference<>(false);

                        event.getMember().ifPresent(m -> mandarin.set(m.getId().asString().equals(StaticStore.MANDARIN_SMELL)));

                        return mc.getId().asString().equals(StaticStore.BOT_COMMANDS) || mandarin.get();
                    }
                }).subscribe(event -> {
                    Message msg = event.getMessage();

                    MessageChannel ch = msg.getChannel().block();

                    event.getMember().ifPresent(m -> {
                        String prefix = StaticStore.getPrefix(m.getId().asString());

                        if(msg.getContent().startsWith(StaticStore.serverPrefix))
                            prefix = StaticStore.serverPrefix;

                        System.out.println(StaticStore.getCommand(msg.getContent(), prefix));

                        if(ch != null) {
                            switch (StaticStore.getCommand(msg.getContent(), prefix)) {
                                case "checkbcu":
                                    new CheckBCU().execute(event);
                                    break;
                                case "bcustat":
                                    new BCUStat().execute(event);
                                    break;
                                case "analyze":
                                    new Analyze(ConstraintCommand.ROLE.MANDARIN).execute(event);
                                    break;
                                case "help":
                                    new Help().execute(event);
                                    break;
                                case "prefix":
                                    new Prefix(ConstraintCommand.ROLE.MEMBER).execute(event);
                                    break;
                                case "serverpre":
                                    new ServerPrefix(ConstraintCommand.ROLE.MOD).execute(event);
                                    break;
                                case "save":
                                    new Save(ConstraintCommand.ROLE.MOD).execute(event);
                                    break;
                            }
                        }
                    });
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

    public static void initialize() {
        if(!StaticStore.initialized) {
            CommonStatic.ctx = new PackContext();
            StaticStore.readServerInfo();

            StaticStore.initialized = true;
        }
    }
}
