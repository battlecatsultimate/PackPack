package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import java.io.File
import kotlin.system.exitProcess

class ReplaceSave : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent) {
        val m = getMember(event) ?: return
        val ch = getChannel(event) ?: return
        val msg = getMessage(event)

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid")) {
            return
        }

        if (msg.attachments.isEmpty()) {
            replyToMessageSafely(ch, "Please provide save file that will be replaced!", msg) { a -> a }

            return
        }

        val saveFile = msg.attachments.find { a -> a.fileName == "cardSave.json" }

        val folder = File("./temp")

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/ReplaceSave::doSomething - Failed to create folder : ${folder.absolutePath}")

            return
        }

        CardBot.forceReplace = true

        try {
            val realFile = File("./data/cardSave.json")

            val downloader = StaticStore.getDownloader(saveFile, folder)

            val tempFile = downloader.target

            downloader.run { _ -> }

            if (realFile.exists() && !realFile.delete()) {
                replyToMessageSafely(ch, "Failed to delete save file before replacing...", getMessage(event)) { a -> a }

                CardBot.forceReplace = false

                return
            }

            if (tempFile.exists() && !tempFile.renameTo(realFile)) {
                replyToMessageSafely(ch, "Failed to move downloaded save file to destination place...", getMessage(event)) { a -> a }

                CardBot.forceReplace = false

                return
            }

            replyToMessageSafely(ch, "Successfully replaced save file. Bot will be turned off", getMessage(event), { a -> a }, {
                event.jda.shutdown()
                StaticStore.saver.cancel()

                exitProcess(0)
            })
        } catch (e: Exception) {
            StaticStore.logger.uploadErrorLog(e, "E/ReplaceSave::doSomething - Failed to replace save file")

            CardBot.forceReplace = false

            replyToMessageSafely(ch, "Failed to replace save file...", getMessage(event)) { a -> a }
        }
    }
}