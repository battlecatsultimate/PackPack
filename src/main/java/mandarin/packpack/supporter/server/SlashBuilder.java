package mandarin.packpack.supporter.server;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class SlashBuilder {
    private static final ArrayList<ApplicationCommandRequest> requests = new ArrayList<>();

    @SuppressWarnings("ConstantConditions")
    public static void build(GatewayDiscordClient client) {
        RestClient rest = client.getRestClient();
        long appID = rest.getApplicationId().block();

        getCommandCreation("fs", "Show stat of unit",
                List.of(
                        new SlashOption("name", "Name of unit", true, SlashOption.TYPE.STRING),
                        new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                        new SlashOption("talent", "Apply talent to this unit if bot can", false, SlashOption.TYPE.BOOLEAN),
                        new SlashOption("level", "Level of this unit", false, SlashOption.TYPE.INT),
                        new SlashOption("talent_lv_1", "First talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                        new SlashOption("talent_lv_2", "Second talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                        new SlashOption("talent_lv_3", "Third talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                        new SlashOption("talent_lv_4", "Fourth talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                        new SlashOption("talent_lv_5", "Fifth talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT)
                )
        );

        getCommandCreation("es", "Show stat of enemy",
                List.of(
                        new SlashOption("name", "Name of enemy", true, SlashOption.TYPE.STRING),
                        new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                        new SlashOption("magnification", "Set magnification of this enemy", false, SlashOption.TYPE.INT),
                        new SlashOption("atk_magnification", "Set magnification of attack of this enemy", false, SlashOption.TYPE.INT)
                )
        );

        applyCreatedSlashCommands(rest, appID);

        client.on(new ReactiveEventAdapter() {

            @NotNull
            @Override
            public Publisher<?> onInteractionCreate(@NotNull InteractionCreateEvent event) {
                String command = event.getCommandName();

                switch (command) {
                    case "fs":
                        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("Working on it"));
                    case "es":
                        return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage("WIP2"));
                }

                return Mono.empty();
            }
        }).subscribe();

        printAllCommandData(rest);
    }

    @SuppressWarnings("ConstantConditions")
    private static void printAllCommandData(RestClient client) {
        long appID = client.getApplicationId().block();

        System.out.println("Applicatoin ID : "+appID);

        Flux<ApplicationCommandData> commands = client.getApplicationService().getGlobalApplicationCommands(appID);

        ApplicationCommandData data;

        long size = commands.count().block();

        for(long i = 0; i < size; i++) {
            data = commands.elementAt((int) i).block();

            if(data != null) {
                System.out.println("--------------------\n\nName : "+data.name()+"\nDescription : "+data.description()+"\nID : "+data.id());

                if(!data.options().isAbsent()) {
                    System.out.println("\n- Options -\n");

                    List<ApplicationCommandOptionData> options = data.options().get();

                    for(ApplicationCommandOptionData option : options) {
                        String type;

                        if(option.type() == ApplicationCommandOptionType.BOOLEAN.getValue())
                            type = "Boolean";
                        else if(option.type() == ApplicationCommandOptionType.INTEGER.getValue())
                            type = "Integer";
                        else if(option.type() == ApplicationCommandOptionType.STRING.getValue())
                            type = "String";
                        else if(option.type() == ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue())
                            type = "Subcommand Group";
                        else if(option.type() == ApplicationCommandOptionType.SUB_COMMAND.getValue())
                            type = "Subcommand";
                        else if(option.type() == ApplicationCommandOptionType.CHANNEL.getValue())
                            type = "Channel";
                        else if(option.type() == ApplicationCommandOptionType.ROLE.getValue())
                            type = "Role";
                        else if(option.type() == ApplicationCommandOptionType.USER.getValue())
                            type = "User";
                        else
                            type = "Unknown : "+option.type();

                        System.out.println("---\nName : "+option.name()+"\nDescription : "+option.description()+"\nRequired : "+!option.required().isAbsent()+"\nType : "+type);
                    }

                    System.out.println("---");
                }

                System.out.println("\n--------------------");
            }
        }
    }

    private static void getCommandCreation(@NotNull String name, @NotNull String description, @Nullable List<SlashOption> options) {
        ImmutableApplicationCommandRequest.Builder builder = ApplicationCommandRequest.builder();

        builder.name(name)
                .description(description);

        if(options != null) {
            for(SlashOption option : options) {
                option.apply(builder);
            }
        }

        requests.add(builder.build());
    }

    private static void applyCreatedSlashCommands(RestClient client, long appID) {
        for(ApplicationCommandRequest request : requests) {
            try {
                client.getApplicationService()
                        .createGlobalApplicationCommand(appID, request).block();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        requests.clear();
    }
}

class SlashOption {
    enum TYPE {
        INT(ApplicationCommandOptionType.INTEGER.getValue()),
        BOOLEAN(ApplicationCommandOptionType.BOOLEAN.getValue()),
        STRING(ApplicationCommandOptionType.STRING.getValue()),
        ROLE(ApplicationCommandOptionType.ROLE.getValue()),
        CHANNEL(ApplicationCommandOptionType.CHANNEL.getValue());

        int type;

        TYPE(int type) {
            this.type = type;
        }
    }

    @NotNull
    private final String name;
    private final boolean required;
    @NotNull
    private final String description;
    @NotNull
    private final TYPE type;

    public SlashOption(@NotNull String name, @NotNull String description, boolean required, @NotNull TYPE type) {
        this.name = name;
        this.required = required;
        this.description = description;
        this.type = type;
    }

    public void apply(ImmutableApplicationCommandRequest.Builder builder) {
        builder.addOption(ApplicationCommandOptionData.builder()
            .name(name)
            .description(description)
            .type(type.type)
            .required(required)
            .build()
        );
    }
}
