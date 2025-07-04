package mandarin.card.supporter.holder.card

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageDetector
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import java.io.File
import java.nio.file.Files

class CardFileHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card?) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN), MessageDetector {
    private var downloading = false

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {

    }

    override fun clean() {

    }

    override fun onExpire() {

    }

    override fun onMessageDetected(msg: Message) {
        if (msg.attachments.isEmpty()) {
            return
        }

        val pickedAttachment = msg.attachments.find { attachment ->
            val fileSegments = attachment.fileName.split(".")

            if (fileSegments.size < 2)
                return@find false

            val extension = fileSegments[1]

            return@find extension.lowercase() in CardData.supportedFileFormat
        } ?: return

        message.editMessage("Downloading file... : ${pickedAttachment.fileName}")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()

        val folder = File("./temp/")

        if (!folder.exists() && !folder.mkdir()) {
            downloading = false

            message.editMessage("Failed to create container folder... Contact developer")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        val downloader = StaticStore.getDownloader(pickedAttachment, folder)

        try {
            downloader.run {  }
        } catch (e: Exception) {
            StaticStore.logger.uploadErrorLog(e, "E/CardFileHolder::onMessageDetected - Failed to download file : ${pickedAttachment.fileName}")

            return
        } finally {
            if (downloader.temp.exists() && !Files.deleteIfExists(downloader.temp.toPath())) {
                StaticStore.logger.uploadLog("W/CardFileHolder::onMessageDetected - Failed to delete temporary file : ${downloader.temp.absolutePath}")
            }
        }

        downloading = false

        if (!downloader.target.exists()) {
            message.editMessage("Failed to download file... Contact developer")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        } else if (Files.size(downloader.target.toPath()) > Message.MAX_FILE_SIZE) {
            message.editMessage("File is too large! Please optimize the file size and re-upload again")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            if (!Files.deleteIfExists(downloader.target.toPath())) {
                StaticStore.logger.uploadLog("W/CardFileHolder::onMessageDetected - Failed to delete file : ${downloader.target.absolutePath}")
            }

            return
        }

        message.editMessage("Download complete!")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()

        msg.delete().queue()

        if (card == null) {
            val c = Card((CardData.cards.maxOfOrNull { c -> c.id } ?: 0) + 1, CardData.Tier.COMMON, "Card", downloader.target)

            parent?.connectTo(CardModifyHolder(authorMessage, userID, channelID, message, c, true))

            end(false)
        } else {
            if (!card.cardImage.delete()) {
                message.editMessage("Failed to delete original card image file! Contact developer").queue()

                if (!downloader.target.delete()) {
                    StaticStore.logger.uploadLog("W/CardFileHolder::onMessageDetected - Failed to delete downloaded card file : ${downloader.target.absolutePath}")
                }

                return
            }

            if (!downloader.target.renameTo(card.cardImage)) {
                message.editMessage("Failed to move downloaded card image file to proper place! Contact developer").queue()

                if (!downloader.target.delete()) {
                    StaticStore.logger.uploadLog("W/CardFileHolder::onMessageDetected - Failed to delete downloaded card file : ${downloader.target.absolutePath}")
                }

                return
            }

            goBack()
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents()
            .setFiles()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "Please upload the card file"
    }
}