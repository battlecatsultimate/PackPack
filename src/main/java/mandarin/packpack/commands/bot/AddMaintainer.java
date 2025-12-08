package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class AddMaintainer extends ConstraintCommand {
    public AddMaintainer(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        ShardManager manager = loader.getClient().getShardManager();

        if (manager == null) {
            return;
        }

        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Format : `p!am [User ID]`");

            return;
        }

        String id = contents[1];

        if (id.matches("<@\\d+>")) {
            id = id.replace("<@", "").replace(">", "");
        }

        if (!StaticStore.isNumeric(id)) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "User ID must be numeric");

            return;
        }

        if (!validUser(manager, StaticStore.safeParseLong(id))) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Failed to found user <@%s> (%s)".formatted(id, id));

            return;
        }

        if (StaticStore.maintainers.contains(id)) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "User <@%s> (%s) is already maintainer of this bot".formatted(id, id));

            return;
        }

        StaticStore.maintainers.add(id);

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "User <@%s> (%s) is now a maintainer of this bot".formatted(id, id));
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
