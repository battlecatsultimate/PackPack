package mandarin.packpack.supporter.server.slash;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.InteractionCreateEvent;
import discord4j.discordjson.json.*;
import discord4j.rest.RestClient;
import discord4j.rest.util.ApplicationCommandOptionType;
import mandarin.packpack.commands.bc.EnemyStat;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.SpamPrevent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

        getCommandCreation("si", "Show stat of stage",
                List.of(
                        new SlashOption("name", "Name of stage", true, SlashOption.TYPE.STRING),
                        new SlashOption("stage_map", "Name of stage map", false, SlashOption.TYPE.STRING),
                        new SlashOption("map_collection", "Name of map collection", false, SlashOption.TYPE.STRING),
                        new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                        new SlashOption("level", "Set level (New name of star) to this stage", false, SlashOption.TYPE.INT)
                )
        );

        applyCreatedSlashCommands(rest, appID);

        client.on(new ReactiveEventAdapter() {

            @NotNull
            @Override
            public Publisher<?> onInteractionCreate(@NotNull InteractionCreateEvent event) {
                if(!event.getInteraction().getData().member().isAbsent()) {
                    SpamPrevent spam;

                    MemberData m = event.getInteraction().getData().member().get();

                    if(StaticStore.spamData.containsKey(m.user().id().asString())) {
                        spam = StaticStore.spamData.get(m.user().id().asString());

                        Mono<?> prevented = spam.isPrevented(event);

                        if(prevented != null)
                            return prevented;
                    } else {
                        spam = new SpamPrevent();

                        StaticStore.spamData.put(m.user().id().asString(), spam);
                    }
                }

                String command = event.getCommandName();

                switch (command) {
                    case "fs":
                        WebhookBuilder request = FormStat.getInteractionWebhook(event);

                        if(request != null) {
                            return event.acknowledge()
                                    .then(event.getInteractionResponse().createFollowupMessage(request.build(), true))
                                    .flatMap(m -> Mono.create(v -> request.doAdditionalJob(client, m)))
                                    .then(Mono.create(m -> request.finishJob(true)))
                                    .doOnError(e -> {
                                        e.printStackTrace();
                                        request.finishJob(true);
                                    });
                        }

                        break;
                    case "es":
                        request = EnemyStat.getInteractionWebhook(event.getInteraction().getData());

                        if(request != null) {
                            return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(request.build(), true))
                                    .then(Mono.create(m -> request.finishJob(true)))
                                    .doOnError(e -> {
                                        e.printStackTrace();
                                        request.finishJob(true);
                                    });
                        }

                        break;
                    case "si":
                        request = StageInfo.getInteractionWebhook(event);

                        if(request != null) {
                            return event.acknowledge().then(event.getInteractionResponse().createFollowupMessage(request.build(), true))
                                    .flatMap(m -> Mono.create(v -> request.doAdditionalJob(client, m)))
                                    .then(Mono.create(m -> request.finishJob(true)))
                                    .doOnError(e -> {
                                        e.printStackTrace();
                                        request.finishJob(true);
                                    });
                        }
                }

                return Mono.empty();
            }
        }).subscribe();

        printAllCommandData(rest);
    }

    public static WebhookBuilder getWebhookRequest(@NotNull Consumer<WebhookBuilder> handler) {
        WebhookBuilder builder = new WebhookBuilder();

        handler.accept(builder);

        return builder;
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
