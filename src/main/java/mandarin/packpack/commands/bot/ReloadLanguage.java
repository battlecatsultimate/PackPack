package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import org.jetbrains.annotations.Nullable;

public class ReloadLanguage extends ConstraintCommand {
    public ReloadLanguage(ROLE role, int lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        MessageChannel ch = getChannel(event);

        if(ch == null)
            return;

        Message msg = getRepliedMessageSafely(ch, "Reloading language data...", getMessage(event), a -> a);

        LangID.initialize();

        msg.editMessage("Successfully reloaded language data!").mentionRepliedUser(false).queue();
    }
}
