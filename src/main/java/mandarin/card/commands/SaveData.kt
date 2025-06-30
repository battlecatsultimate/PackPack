package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.Logger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class SaveData : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid")) {
            return
        }

        val link = StaticStore.backup.uploadBackup(Logger.BotInstance.CARD_DEALER)

        if (link.isBlank()) {
            replyToMessageSafely(loader.channel, "Failed to upload save file to google drive", loader.message) { a -> a }

            return
        }

        u.openPrivateChannel().queue { pv ->
            pv.sendMessage("Send save data : $link").queue()
        }
    }
}