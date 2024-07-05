package mandarin.packpack.supporter.server.holder;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;

public interface MessageDetector {
    void onMessageDetected(@NotNull Message message);
}
