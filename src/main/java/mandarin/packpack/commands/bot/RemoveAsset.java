package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoveAsset extends ConstraintCommand {
    public RemoveAsset(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        String[] contents = loader.getContent().split(" ");

        if (contents.length < 2) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Format : `p!ra <-r|-regex> [ID]");

            return;
        }

        boolean regex = contents[1].equals("-r") || contents[1].equals("-regex");
        int startIndex = regex ? 2 : 1;

        if (contents.length < startIndex + 1) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Format : `p!ra <-r|-regex> [ID]");

            return;
        }

        StringBuilder builder = new StringBuilder();

        for (int i = startIndex; i < contents.length; i++) {
            builder.append(contents[i]).append(" ");
        }

        String id = builder.toString().trim();

        if (regex) {
            StaticStore.assetManager.removeAssetRegex(id);
        } else {
            StaticStore.assetManager.removeAsset(id);
        }

        replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Successfully removed " + (regex ? "regex " : "") + id);
    }
}
