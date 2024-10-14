package mandarin.card.commands

import com.google.gson.JsonParser
import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import java.io.File
import java.io.FileReader
import java.nio.file.Files

class InjectInventory : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid")) {
            return
        }

        val msg = loader.message
        val reference = msg.referencedMessage

        val ch = loader.channel

        val attachment = if (reference != null) {
            if (reference.attachments.isEmpty()) {
                if (msg.attachments.isEmpty()) {
                    replyToMessageSafely(ch, "Failed to find attachment from referenced message. Please provide the file manually", msg) { a -> a }

                    return
                } else {
                    msg.attachments.find { attachment -> attachment.fileName == "inventory.json" }
                }
            } else {
                reference.attachments.find { attachment -> attachment.fileName == "inventory.json" }
            }
        } else {
            if (msg.attachments.isEmpty()) {
                replyToMessageSafely(ch, "Please provide the inventory json file!", msg) { a -> a }

                return
            } else {
                msg.attachments.find { attachment -> attachment.fileName == "inventory.json" }
            }
        }

        if (attachment == null) {
            replyToMessageSafely(ch, "Failed to find inventory.json file from referenced message. If you want to inject raw file manually, please don't reference the message", msg) { a -> a }

            return
        }

        val id = if (reference != null) {
            if (!reference.contentRaw.matches(Regex("Inventory of <@\\d+> \\(\\d+\\)"))) {
                val potentialID = getUserID(msg.contentRaw.split(" "))

                if (potentialID == -1L) {
                    replyToMessageSafely(ch, "Failed to get user ID from referenced message. Please provide the ID of the user!", msg) { a -> a }

                    return
                } else {
                    potentialID
                }
            } else {
                StaticStore.safeParseLong(reference.contentRaw.split("<@")[1].split(">")[0])
            }
        } else {
            val potentialID = getUserID(msg.contentRaw.split(" "))

            if (potentialID == -1L) {
                replyToMessageSafely(ch, "Failed to get user ID from referenced message. Please provide the ID of the user!", msg) { a -> a }

                return
            } else {
                potentialID
            }
        }

        val downloader = StaticStore.getDownloader(attachment, File("./temp"))

        downloader.run {  }

        if (!downloader.target.exists()) {
            replyToMessageSafely(ch, "Failed to download file from the attachments", msg) { a -> a }

            return
        }

        val reader = FileReader(downloader.target)

        val jsonData = JsonParser.parseReader(reader)

        reader.close()

        if (downloader.target.exists()) {
            Files.delete(downloader.target.toPath())
        }

        replyToMessageSafely(ch, "Are you sure you want to inject this inventory to <@$id> ($id)?", msg, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { message ->
            StaticStore.putHolder(u.id, ConfirmButtonHolder(msg, u.id, ch.id, message, CommonStatic.Lang.Locale.EN) {
                val inventory = Inventory.readInventory(id, jsonData.asJsonObject)

                CardData.inventories[id] = inventory

                CardBot.saveCardData()

                replyToMessageSafely(ch, "Successfully injected inventory data!", msg) { a -> a }
            })
        }
    }

    private fun getUserID(contents: List<String>) : Long {
        return StaticStore.safeParseLong(contents.map { s -> s.replace("<@", "").replace(">", "") }.find { s -> StaticStore.isNumeric(s) } ?: "-1")
    }
}