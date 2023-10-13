package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

public class ReloadLanguage extends ConstraintCommand {
    public ReloadLanguage(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

       replyToMessageSafely(ch, "Reloading language data...", loader.getMessage(), a -> a, msg -> {
            LangID.initialize();

            msg.editMessage("Successfully reloaded language data!").mentionRepliedUser(false).queue();
        });
    }
}
