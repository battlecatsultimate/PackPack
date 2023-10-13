package mandarin.card.commands

import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File

class SaveData : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid")) {
            return
        }

        m.user.openPrivateChannel().queue { pv ->
            pv.sendMessage("Send save data")
                .addFiles(FileUpload.fromData(File("./data/cardSave.json")))
                .queue()
        }
    }
}