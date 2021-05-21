package mandarin.packpack.supporter.server.slash;

import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import mandarin.packpack.supporter.StaticStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class SlashOption {
    enum TYPE {
        INT(ApplicationCommandOptionType.INTEGER.getValue()),
        BOOLEAN(ApplicationCommandOptionType.BOOLEAN.getValue()),
        STRING(ApplicationCommandOptionType.STRING.getValue()),
        ROLE(ApplicationCommandOptionType.ROLE.getValue()),
        CHANNEL(ApplicationCommandOptionType.CHANNEL.getValue()),
        SUB_COMMAND(ApplicationCommandOptionType.SUB_COMMAND.getValue()),
        GROUP(ApplicationCommandOptionType.SUB_COMMAND_GROUP.getValue());

        int type;

        TYPE(int type) {
            this.type = type;
        }
    }

    public static ApplicationCommandInteractionOptionData getOption(List<ApplicationCommandInteractionOptionData> options, String name) {
        for(ApplicationCommandInteractionOptionData option : options) {
            if(option.name().equals(name)) {
                return option;
            }
        }

        return null;
    }

    @NotNull
    public static String getStringOption(List<ApplicationCommandInteractionOptionData> options, String name) {
        ApplicationCommandInteractionOptionData data = getOption(options, name);

        if(data == null)
            return "";

        if(data.type() == TYPE.STRING.type && !data.value().isAbsent())
            return data.value().get();

        return "";
    }

    public static boolean getBooleanOption(List<ApplicationCommandInteractionOptionData> options, String name) {
        ApplicationCommandInteractionOptionData data = getOption(options, name);

        if(data == null)
            return false;

        if(data.type() == TYPE.BOOLEAN.type && !data.value().isAbsent()) {
            String value = data.value().get();

            return value.toLowerCase(Locale.ENGLISH).equals("true");
        }

        return false;
    }

    public static int getIntOption(List<ApplicationCommandInteractionOptionData> options, String name) {
        ApplicationCommandInteractionOptionData data = getOption(options, name);

        if(data == null)
            return -1;

        if(data.type() == TYPE.INT.type && !data.value().isAbsent())
            return StaticStore.safeParseInt(data.value().get());

        return -1;
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
