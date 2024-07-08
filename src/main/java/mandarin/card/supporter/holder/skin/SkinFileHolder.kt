package mandarin.card.supporter.holder.skin

import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageDetector
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.io.File
import java.nio.file.Files

class SkinFileHolder : ComponentHolder, MessageDetector {
    private val message: Message
    private val card: Card
    private val skin: Skin?

    private var downloading = false

    constructor(author: Message, channelID: String, message: Message, card: Card) : super(author, channelID, message) {
        this.message = message
        this.skin = null
        this.card = card
    }

    constructor(author: Message, channelID: String, message: Message, card: Card, skin: Skin) : super(author, channelID, message) {
        this.message = message
        this.skin = skin
        this.card = card
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "back" -> {
                registerPopUp(event,
                    if (skin == null)
                        "Are you sure you want to stop creating new skin?"
                    else
                        "Are you sure you want to cancel replacing skin file?"
                    , LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    goBack(e)
                }, LangID.EN))
            }
            "close" -> {
                registerPopUp(event, "Are you sure you want to stop creating new skin?", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferEdit()
                        .setContent("Management closed")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    expired = true
                }, LangID.EN))
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

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
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()

        val folder = File("./temp/")

        if (!folder.exists() && !folder.mkdir()) {
            downloading = false

            message.editMessage("Failed to create container folder... Contact developer")
                .setComponents(getComponents())
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        }

        val downloader = StaticStore.getDownloader(pickedAttachment, folder)

        try {
            downloader.run {  }
        } catch (e: Exception) {
            StaticStore.logger.uploadErrorLog(e, "E/SkinFileHolder::onMessageDetected - Failed to download file : ${pickedAttachment.fileName}")

            return
        } finally {
            if (downloader.temp.exists() && !Files.deleteIfExists(downloader.temp.toPath())) {
                StaticStore.logger.uploadLog("W/SkinFileHolder::onMessageDetected - Failed to delete temporary file : ${downloader.temp.absolutePath}")
            }
        }

        downloading = false

        if (!downloader.target.exists()) {
            message.editMessage("Failed to download file... Contact developer")
                .setComponents(getComponents())
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            return
        } else if (Files.size(downloader.target.toPath()) > Message.MAX_FILE_SIZE) {
            message.editMessage("File is too large! Please optimize the file size and re-upload again")
                .setComponents(getComponents())
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            if (!Files.deleteIfExists(downloader.target.toPath())) {
                StaticStore.logger.uploadLog("W/SkinFileHolder::onMessageDetected - Failed to delete file : ${downloader.target.absolutePath}")
            }

            return
        }

        message.editMessage("Download complete!")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()

        msg.delete().queue()

        if (skin == null) {
            val skin = Skin(card, downloader.target)

            connectTo(SkinModifyHolder(authorMessage, channelID, message, skin, true))
        } else {
            skin.updateFile(downloader.target, authorMessage.jda, skin in CardData.skins)

            goBack()
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder?) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "Please upload the file\n### Supported File Type : ${CardData.supportedFileFormat.map { s -> s.uppercase() }.joinToString(", ")}"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (skin == null) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK).withDisabled(downloading),
                Button.danger("close", "Close").withEmoji(EmojiStore.CROSS).withDisabled(downloading)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK).withDisabled(downloading)
            ))
        }

        return result
    }
}