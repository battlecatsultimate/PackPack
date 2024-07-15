package mandarin.packpack.commands;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OptIn extends ConstraintCommand {
    public OptIn(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), "Format : `p!uoo [Member ID]`", loader.getMessage(), a -> a);

            return;
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(loader.getChannel(), "User ID isn't numeric!", loader.getMessage(), a -> a);

            return;
        }

        long id = StaticStore.safeParseLong(contents[1]);

        if (StaticStore.optoutMembers.contains(String.valueOf(id))) {
            StaticStore.optoutMembers.remove(String.valueOf(id));

            replyToMessageSafely(loader.getChannel(), "User <@" + id + "> has opt in!", loader.getMessage(), a -> a);
        } else {
            replyToMessageSafely(loader.getChannel(), "This user hasn't opt out yet", loader.getMessage(), a -> a);
        }
    }
}
