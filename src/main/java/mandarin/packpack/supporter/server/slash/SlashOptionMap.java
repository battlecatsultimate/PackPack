package mandarin.packpack.supporter.server.slash;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlashOptionMap {
    Map<String, SlashOptionData> optionMap = new HashMap<>();

    public SlashOptionMap(List<OptionMapping> optionMappings) {
        for (int i = 0; i < optionMappings.size(); i++) {
            OptionMapping option = optionMappings.get(i);

            Class<?> type;
            Object data;

            switch (option.getType()) {
                case STRING -> {
                    type = String.class;
                    data = option.getAsString();
                }
                case INTEGER -> {
                    type = int.class;
                    data = (int) option.getAsLong();
                }
                case BOOLEAN -> {
                    type = boolean.class;
                    data = option.getAsBoolean();
                }
                case USER -> {
                    type = User.class;
                    data = option.getAsUser();
                }
                case CHANNEL -> {
                    type = GuildChannelUnion.class;
                    data = option.getAsChannel();
                }
                case ROLE -> {
                    type = Role.class;
                    data = option.getAsRole();
                }
                case ATTACHMENT -> {
                    type = Message.Attachment.class;
                    data = option.getAsAttachment();
                }
                default -> throw new IllegalStateException("E/SlashOptionMap::init - Unknown slash command option type : %s".formatted(option.getType()));
            }

            optionMap.put(option.getName(), new SlashOptionData(type, data));
        }
    }

    @Nonnull
    public <T> T getOption(String name, @Nonnull T defaultValue) {
        SlashOptionData data = optionMap.get(name);

        if (data == null || data.data() == null) {
            return defaultValue;
        }

        Class<?> c = MethodType.methodType(defaultValue.getClass()).unwrap().returnType();

        if (c != data.type()) {
            return defaultValue;
        }

        //noinspection unchecked
        return (T) data.data();
    }
}
