package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.holder.modal.YDKEFileModalHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit

class YDKEUploadHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMicros(1))
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "file" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val fileName = event.values.first()

                val modal = Modal.create("upload", "Upload YDKE File")
                    .addComponents(TextDisplay.of("Please upload YDKE file"))
                    .addComponents(Label.of("File", AttachmentUpload.create("file").setMaxValues(1).setRequired(true).build()))
                    .build()

                event.replyModal(modal).queue()

                connectTo(YDKEFileModalHolder(authorMessage, userID, channelID, message, fileName))
            }
            "close" -> {
                event.editComponents(TextDisplay.of("Closed YDKE file uploader"))
                    .useComponentsV2()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessageComponents(TextDisplay.of("YDKE file uploader expired"))
            .useComponentsV2()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setComponents(getContainer())
            .useComponentsV2()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessageComponents(getContainer())
            .useComponentsV2()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContainer() : Container {
        val components = ArrayList<ContainerChildComponent>()

        components.add(TextDisplay.of("Please select YDKE file type that you want to upload"))
        components.add(Separator.create(true, Separator.Spacing.LARGE))

        val options = ArrayList<SelectOption>()

        options.add(SelectOption.of("cards.cdb", "cards.cdb").withDescription("CDB data for general YDKE"))
        options.add(SelectOption.of("BCTC.cdb", "BCTC.cdb").withDescription("CDB data for BCTC custom cards"))
        options.add(SelectOption.of("Normal.lflist.conf", "Normal.lflist.conf").withDescription("White list data for normal case"))
        options.add(SelectOption.of("Tournament.lflist.conf", "Tournament.lflist.conf").withDescription("White list data for tournament case"))
        options.add(SelectOption.of("BCE.lflist.conf", "BCE.lflist.conf").withDescription("White list data for BCE case"))

        components.add(ActionRow.of(StringSelectMenu.create("file").addOptions(options).setPlaceholder("Select file type to upload").build()))
        components.add(ActionRow.of(Button.secondary("close", "Close").withEmoji(EmojiStore.CROSS)))

        return Container.of(components)
    }
}