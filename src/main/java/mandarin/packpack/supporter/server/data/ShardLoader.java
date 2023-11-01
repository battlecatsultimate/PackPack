package mandarin.packpack.supporter.server.data;

import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.ArrayList;
import java.util.List;

public class ShardLoader {
    public final List<Guild> emojiArchives = new ArrayList<>();
    public final Guild supportServer;

    public boolean fullyLoaded;

    public ShardLoader(ShardManager manager) {
        fullyLoaded = true;

        for(String id : StaticStore.EMOJI_ARCHIVES) {
            Guild g = manager.getGuildById(id);

            if(g == null) {
                fullyLoaded = false;

                break;
            }

            emojiArchives.add(g);
        }

        supportServer = manager.getGuildById(StaticStore.SUPPORT_SERVER);

        if (supportServer == null) {
            fullyLoaded = false;
        }
    }
}
