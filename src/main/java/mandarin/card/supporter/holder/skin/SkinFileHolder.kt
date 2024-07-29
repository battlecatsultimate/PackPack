package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageDetector
import mandarin.packpack.supporter.server.holder.MessageUpdater
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class SkinFileHolder : ComponentHolder, MessageDetector, MessageUpdater {
    private val card: Card
    private val skin: Skin?

    private var downloading = false

    constructor(author: Message, channelID: String, message: Message, card: Card) : super(author, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.skin = null
        this.card = card
    }

    constructor(author: Message, channelID: String, message: Message, card: Card, skin: Skin) : super(author, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.skin = skin
        this.card = card
    }

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Skin manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "back" -> {
                registerPopUp(event,
                    if (skin == null)
                        "Are you sure you want to stop creating new skin?"
                    else
                        "Are you sure you want to cancel replacing skin file?"
                )

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    goBack(e)
                }, CommonStatic.Lang.Locale.EN))
            }
            "close" -> {
                registerPopUp(event, "Are you sure you want to stop creating new skin?")

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferEdit()
                        .setContent("Management closed")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    end()
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onMessageUpdated(message: Message) {
        this.message = message
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

            println("PREPARE : " + message.attachments)

            goBack()
        }
    }

    override fun onConnected(event: IMessageEditCallback) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        if (event !is GenericComponentInteractionCreateEvent)
            return

        var builder = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
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