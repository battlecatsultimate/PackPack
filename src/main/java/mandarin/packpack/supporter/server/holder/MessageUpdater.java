package mandarin.packpack.supporter.server.holder;

import net.dv8tion.jda.api.entities.Message;
import javax.annotation.Nonnull;

public interface MessageUpdater {
    void onMessageUpdated(@Nonnull Message message);
}
