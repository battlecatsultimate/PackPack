package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GrabLanguage extends ConstraintCommand {
    public GrabLanguage(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String[] contents = loader.getContent().split(" ", 4);

        if(contents.length < 3) {
            replyToMessageSafely(ch, "Format : `p!gl [Locale Number] [ID]`", loader.getMessage(), a -> a);

            return;
        }

        int l = LangID.EN;

        if(StaticStore.isNumeric(contents[1])) {
            l = StaticStore.safeParseInt(contents[1]);
        }

        if(contents.length > 3) {
            try {
                replyToMessageSafely(ch, String.format(LangID.getStringByID(contents[2], l), (Object[]) contents[3].split("\\\\")), loader.getMessage(), a -> a);
            } catch (Exception e) {
                replyToMessageSafely(ch, LangID.getStringByID(contents[2], l), loader.getMessage(), a -> a);
            }
        } else {
            replyToMessageSafely(ch, LangID.getStringByID(contents[2], l), loader.getMessage(), a -> a);
        }
    }
}
