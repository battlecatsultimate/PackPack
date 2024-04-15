package mandarin.packpack.supporter.server.slash;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SlashOption {
    @SuppressWarnings("unused")
    public enum TYPE {
        INT(OptionType.INTEGER),
        BOOLEAN(OptionType.BOOLEAN),
        STRING(OptionType.STRING),
        ROLE(OptionType.ROLE),
        CHANNEL(OptionType.CHANNEL),
        SUB_COMMAND(OptionType.SUB_COMMAND),
        GROUP(OptionType.SUB_COMMAND_GROUP);

        final OptionType type;

        TYPE(OptionType type) {
            this.type = type;
        }
    }

    public static OptionMapping getOption(List<OptionMapping> options, @NotNull String name) {
        for(OptionMapping option : options) {
            if(option.getName().equals(name)) {
                return option;
            }
        }

        return null;
    }

    @NotNull
    public static String getStringOption(List<OptionMapping> options, String name, @NotNull String def) {
        OptionMapping data = getOption(options, name);

        if(data == null)
            return def;

        if(data.getType() == TYPE.STRING.type)
            return data.getAsString();

        return def;
    }

    public static boolean getBooleanOption(List<OptionMapping> options, String name, boolean def) {
        OptionMapping data = getOption(options, name);

        if(data == null)
            return def;

        if(data.getType() == TYPE.BOOLEAN.type) {
            return data.getAsBoolean();
        }

        return def;
    }

    public static int getIntOption(List<OptionMapping> options, String name, int def) {
        OptionMapping data = getOption(options, name);

        if(data == null)
            return def;

        if(data.getType() == TYPE.INT.type)
            return StaticStore.safeParseInt(data.getAsString());

        return def;
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

    public CommandCreateAction apply(CommandCreateAction action) {
        return action.addOption(type.type, name, description, required);
    }
}
