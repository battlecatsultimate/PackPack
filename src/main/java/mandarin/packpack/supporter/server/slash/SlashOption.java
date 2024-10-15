package mandarin.packpack.supporter.server.slash;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;

import javax.annotation.Nonnull;

public class SlashOption {
    @SuppressWarnings("unused")
    public enum TYPE {
        INT(OptionType.INTEGER),
        BOOLEAN(OptionType.BOOLEAN),
        STRING(OptionType.STRING),
        USER(OptionType.USER),
        ROLE(OptionType.ROLE),
        CHANNEL(OptionType.CHANNEL),
        ATTACHMENT(OptionType.ATTACHMENT),
        SUB_COMMAND(OptionType.SUB_COMMAND),
        GROUP(OptionType.SUB_COMMAND_GROUP);

        final OptionType type;

        TYPE(OptionType type) {
            this.type = type;
        }
    }

    @Nonnull
    private final String name;
    private final boolean required;
    private final boolean autoComplete;
    @Nonnull
    private final String description;
    @Nonnull
    private final TYPE type;

    public SlashOption(@Nonnull String name, @Nonnull String description, boolean required, @Nonnull TYPE type) {
        this.name = name;
        this.required = required;
        this.autoComplete = false;
        this.description = description;
        this.type = type;
    }

    public SlashOption(@Nonnull String name, @Nonnull String description, boolean required, @Nonnull TYPE type, boolean autoComplete) {
        this.name = name;
        this.required = required;
        this.autoComplete = autoComplete;
        this.description = description;
        this.type = type;
    }

    public CommandCreateAction apply(CommandCreateAction action) {
        return action.addOption(type.type, name, description, required, autoComplete);
    }
}
