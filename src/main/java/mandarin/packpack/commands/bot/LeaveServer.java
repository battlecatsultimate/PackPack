package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LeaveServer extends ConstraintCommand {
    public LeaveServer(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), "Format : `p!ls [Guild ID]`", loader.getMessage(), a -> a);

            return;
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(loader.getChannel(), "ID must be numeric", loader.getMessage(), a -> a);

            return;
        }

        long id = StaticStore.safeParseLong(contents[1]);

        ShardManager manager = loader.getClient().getShardManager();

        if (manager == null) {
            replyToMessageSafely(loader.getChannel(), "Failed to retrieve shard manager", loader.getMessage(), a -> a);

            return;
        }

        if (StaticStore.bannedServer.contains(id)) {
            StaticStore.bannedServer.remove(id);

            replyToMessageSafely(loader.getChannel(), "Successfully unbanned the server " + id + "!", loader.getMessage(), a -> a);
        } else {
            StaticStore.bannedServer.add(id);

            Guild g = manager.getGuildById(id);

            if (g != null) {
                g.leave().queue(null, e -> StaticStore.logger.uploadErrorLog(e, "E/AllEventAdapter::onGuildJoin - Failed to perform instant guild leave"));
            }

            replyToMessageSafely(loader.getChannel(), "Successfully banned the server " + id + "!", loader.getMessage(), a -> a);
        }

        StaticStore.saveServerInfo();
    }
}
