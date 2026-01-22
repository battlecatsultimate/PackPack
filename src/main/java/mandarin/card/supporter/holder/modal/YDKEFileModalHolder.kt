package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.YDKEValidator
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.io.File

class YDKEFileModalHolder(author: Message, userID: String, channelID: String, message: Message, private val fileName: String) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "upload")
            return

        val file = getAttachmentFromMap(event.values, "file")

        if (file.isEmpty())
            return

        val attachment = file.first()

        val folder = File("./temp")

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/YDKEFileModalHolder::onEvent - Failed to generate temp folder")

            goBack(event)

            return
        }

        val downloader = StaticStore.getDownloader(attachment, folder)

        downloader.run { _ -> }

        val tempFile = downloader.target

        if (!tempFile.exists()) {
            event.deferReply()
                .setComponents(TextDisplay.of("Failed to download attachment..."))
                .useComponentsV2()
                .setAllowedMentions(arrayListOf())
                .mentionRepliedUser(false)
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val actualFile = File("./data/bctcg/$fileName")

        StaticStore.deleteFile(actualFile, true)

        if (!tempFile.renameTo(actualFile)) {
            event.deferReply()
                .setComponents(TextDisplay.of("Failed to paste downloaded file into proper location..."))
                .useComponentsV2()
                .setAllowedMentions(arrayListOf())
                .mentionRepliedUser(false)
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        event.deferReply()
            .setComponents(TextDisplay.of("Successfully uploaded file!"))
            .useComponentsV2()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .setEphemeral(true)
            .queue()

        YDKEValidator.loadCDBData()

        goBack()
    }

    override fun clean() {

    }
}