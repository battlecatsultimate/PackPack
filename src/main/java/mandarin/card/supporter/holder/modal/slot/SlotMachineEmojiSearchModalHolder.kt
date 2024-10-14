package mandarin.card.supporter.holder.modal.slot

import common.CommonStatic
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineEmojiSearchModalHolder(author: Message, userID: String, channelID: String, message: Message, private val onSelect: (String) -> Unit) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "search")
            return

        val keyword = getValueFromMap(event.values, "keyword")

        onSelect.invoke(keyword)

        event.deferReply().setContent("Successfully filtered emoji with name! : $keyword").setEphemeral(true).queue()
    }
}