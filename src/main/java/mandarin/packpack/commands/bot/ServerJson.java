package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.Logger;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;

import javax.annotation.Nonnull;

public class ServerJson extends ConstraintCommand {
    public ServerJson(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        if (!loader.getUser().getId().equals(StaticStore.MANDARIN_SMELL) && !StaticStore.maintainers.contains(loader.getUser().getId())) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), LangID.getStringByID("bot.denied.reason.noPermission.developer", lang));

            return;
        }

        String link = StaticStore.backup.uploadBackup(Logger.BotInstance.PACK_PACK);

        if (link.isBlank()) {
            replyToMessageSafely(loader.getChannel(), loader.getMessage(), "Failed to upload backup");

            return;
        }

        Message msg = loader.getMessage();

        msg.getAuthor().openPrivateChannel()
                .flatMap(pc -> pc.sendMessage("Sent serverinfo.json via DM : " + link))
                .queue();
    }
}
