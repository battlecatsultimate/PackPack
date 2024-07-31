package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File

class SaveData : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid")) {
            return
        }

        u.openPrivateChannel().queue { pv ->
            pv.sendMessage("Send save data")
                .addFiles(FileUpload.fromData(File("./data/cardSave.json")))
                .queue()
        }
    }
}