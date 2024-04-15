package mandarin.packpack.commands.bot;

import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ServerJson extends ConstraintCommand {
    public ServerJson(ROLE role, int lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void doSomething(@NotNull CommandLoader loader) {
        if (!loader.getUser().getId().equals(StaticStore.MANDARIN_SMELL) && !loader.getUser().getId().equals("195682910269865984")) {
            loader.getChannel().sendMessage(LangID.getStringByID("const_man", lang)).queue();

            return;
        }

        StaticStore.saveServerInfo();

        File f = new File("./data/serverinfo.json");

        if(f.exists()) {
            Message msg = loader.getMessage();

            msg.getAuthor().openPrivateChannel()
                    .flatMap(pc -> pc.sendMessage("Sent serverinfo.json via DM").addFiles(FileUpload.fromData(f, "serverinfo.json")))
                    .queue();
        }
    }
}
