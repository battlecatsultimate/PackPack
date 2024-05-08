package mandarin.packpack.supporter.server.holder.component.config;

import mandarin.packpack.supporter.server.holder.component.ComponentHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;

public class ConfigRoleCustomHolder extends ComponentHolder {
    public ConfigRoleCustomHolder(@NotNull Message author, @NotNull String channelID, @NotNull Message message) {
        super(author, channelID, message);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {

    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {

    }
}
