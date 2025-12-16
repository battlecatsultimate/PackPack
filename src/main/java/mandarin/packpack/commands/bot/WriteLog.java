package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class WriteLog extends ConstraintCommand {
    public WriteLog(ROLE role, CommonStatic.Lang.Locale lang, @Nullable IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        MessageChannel ch = loader.getChannel();

        Logger.writeLog(Logger.BotInstance.PACK_PACK);

        replyToMessageSafely(ch, loader.getMessage(), "Successfully wrote log!");
    }
}
