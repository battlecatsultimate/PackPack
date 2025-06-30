package mandarin.packpack.commands.bot;

import common.CommonStatic;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;

import javax.annotation.Nonnull;
import java.io.File;

public class ServerJson extends ConstraintCommand {
    public ServerJson(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) {
        if (!loader.getUser().getId().equals(StaticStore.MANDARIN_SMELL) && !StaticStore.maintainers.contains(loader.getUser().getId())) {
            loader.getChannel().sendMessage(LangID.getStringByID("bot.denied.reason.noPermission.developer", lang)).queue();

            return;
        }

        String link = StaticStore.backup.uploadBackup();

        if (link.isBlank()) {
            replyToMessageSafely(loader.getChannel(), "Failed to upload backup", loader.getMessage(), a -> a);

            return;
        }

        Message msg = loader.getMessage();

        msg.getAuthor().openPrivateChannel()
                .flatMap(pc -> pc.sendMessage("Sent serverinfo.json via DM : " + link))
                .queue();
    }
}
