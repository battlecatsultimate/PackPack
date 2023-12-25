package mandarin.packpack.supporter.server.slash;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SlashBuilder {
    private static final ArrayList<CommandCreateAction> requests = new ArrayList<>();

    public static void build(JDA client) {
        client.retrieveApplicationInfo().queue(info -> {
            if(info == null)
                return;

            getCommandCreation(client,"fs", "Show stat of unit",
                    List.of(
                            new SlashOption("name", "Name of unit", true, SlashOption.TYPE.STRING),
                            new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("talent", "Apply talent to this unit if bot can", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("extra", "Show extra information", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("treasure", "Show values with treasure applied", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("level", "Level of this unit", false, SlashOption.TYPE.INT),
                            new SlashOption("talent_lv_1", "First talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                            new SlashOption("talent_lv_2", "Second talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                            new SlashOption("talent_lv_3", "Third talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                            new SlashOption("talent_lv_4", "Fourth talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT),
                            new SlashOption("talent_lv_5", "Fifth talent level of this unit, only available when talent mode is on", false, SlashOption.TYPE.INT)
                    )
            );

            getCommandCreation(client,"es", "Show stat of enemy",
                    List.of(
                            new SlashOption("name", "Name of enemy", true, SlashOption.TYPE.STRING),
                            new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("extra", "Show extra information", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("magnification", "Set magnification of this enemy", false, SlashOption.TYPE.INT),
                            new SlashOption("atk_magnification", "Set magnification of attack of this enemy", false, SlashOption.TYPE.INT)
                    )
            );

            getCommandCreation(client,"si", "Show stat of stage",
                    List.of(
                            new SlashOption("name", "Name of stage", true, SlashOption.TYPE.STRING),
                            new SlashOption("stage_map", "Name of stage map", false, SlashOption.TYPE.STRING),
                            new SlashOption("map_collection", "Name of map collection", false, SlashOption.TYPE.STRING),
                            new SlashOption("frame", "Show time info with frame", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("extra", "Show extra information", false, SlashOption.TYPE.BOOLEAN),
                            new SlashOption("level", "Set level (New name of star) to this stage", false, SlashOption.TYPE.INT)
                    )
            );

            applyCreatedSlashCommands();

            printAllCommandData(client);
        });
    }

    private static void printAllCommandData(JDA client) {
        client.retrieveApplicationInfo().queue(info ->
            client.retrieveCommands().queue(commands -> {
                if(info == null)
                    return;

                long appID = info.getIdLong();

                System.out.println("Applicatoin ID : "+appID);

                Command data;

                int size = commands.size();

                for(int i = 0; i < size; i++) {
                    data = commands.get(i);

                    if(data != null) {
                        System.out.println("--------------------\n\nName : "+data.getName()+"\nDescription : "+data.getDescription()+"\nID : "+data.getId());

                        if(!data.getOptions().isEmpty()) {
                            System.out.println("\n- Options -\n");

                            List<Command.Option> options = data.getOptions();

                            for(Command.Option option : options) {
                                String type;

                                if(option.getType() == OptionType.BOOLEAN)
                                    type = "Boolean";
                                else if(option.getType() == OptionType.INTEGER)
                                    type = "Integer";
                                else if(option.getType() == OptionType.STRING)
                                    type = "String";
                                else if(option.getType() == OptionType.SUB_COMMAND_GROUP)
                                    type = "Subcommand Group";
                                else if(option.getType() == OptionType.SUB_COMMAND)
                                    type = "Subcommand";
                                else if(option.getType() == OptionType.CHANNEL)
                                    type = "Channel";
                                else if(option.getType() == OptionType.ROLE)
                                    type = "Role";
                                else if(option.getType() == OptionType.USER)
                                    type = "User";
                                else
                                    type = "Unknown : "+option.getType();

                                System.out.println("---\nName : "+option.getName()+"\nDescription : "+option.getDescription()+"\nRequired : "+option.isRequired()+"\nType : "+type);
                            }

                            System.out.println("---");
                        }

                        System.out.println("\n--------------------");
                    }
                }
            })
        );
    }

    private static void getCommandCreation(JDA client, @NotNull String name, @NotNull String description, @Nullable List<SlashOption> options) {
        CommandCreateAction action = client.upsertCommand(name, description);

        if(options != null) {
            for(SlashOption option : options) {
                action = option.apply(action);
            }
        }

        action = action.setGuildOnly(true);

        requests.add(action);
    }

    private static void applyCreatedSlashCommands() {
        for(CommandCreateAction request : requests) {
            try {
                request.queue();
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/SlashBuilder::applyCreatedSlashCommands - Failed to request command");
            }
        }

        requests.clear();
    }
}
