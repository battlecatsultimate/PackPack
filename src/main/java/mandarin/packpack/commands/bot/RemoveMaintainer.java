package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class RemoveMaintainer extends ConstraintCommand {
    public RemoveMaintainer(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        ShardManager manager = loader.getClient().getShardManager();

        if (manager == null) {
            return;
        }

        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), "Format : `p!am [User ID]`", loader.getMessage(), a -> a);

            return;
        }

        String id = contents[1];

        if (id.matches("<@\\d+>")) {
            id = id.replace("<@", "").replace(">", "");
        }

        if (!StaticStore.isNumeric(id)) {
            replyToMessageSafely(loader.getChannel(), "User ID must be numeric", loader.getMessage(), a -> a);

            return;
        }

        if (!validUser(manager, StaticStore.safeParseLong(id))) {
            replyToMessageSafely(loader.getChannel(), "Failed to found user <@%s> (%s)".formatted(id, id), loader.getMessage(), a -> a);

            return;
        }

        if (!StaticStore.maintainers.contains(id)) {
            replyToMessageSafely(loader.getChannel(), "User <@%s> (%s) is not maintainer of this bot already".formatted(id, id), loader.getMessage(), a -> a);

            return;
        }

        StaticStore.maintainers.remove(id);

        replyToMessageSafely(loader.getChannel(), "User <@%s> (%s) is not a maintainer of this bot now".formatted(id, id), loader.getMessage(), a -> a);
    }

    private boolean validUser(ShardManager manager, long userID) throws Exception {
        AtomicReference<Boolean> userFound = new AtomicReference<>(false);

        CountDownLatch counter = new CountDownLatch(1);

        manager.retrieveUserById(userID).queue(unused -> {
            userFound.set(true);

            counter.countDown();
        }, unused -> counter.countDown());

        counter.await();

        return userFound.get();
    }
}
