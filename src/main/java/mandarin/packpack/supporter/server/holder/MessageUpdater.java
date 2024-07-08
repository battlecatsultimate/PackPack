package mandarin.packpack.supporter.server.holder;

import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;

public interface MessageUpdater {
    void onMessageUpdated(@NotNull Message message);
}
