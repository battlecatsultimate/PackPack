package mandarin.card.supporter.holder.modal

import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineEmojiSearchModalHolder(author: Message, channelID: String, message: Message, private val onSelect: (String) -> Unit) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "search")
            return

        val keyword = getValueFromMap(event.values, "keyword")

        onSelect.invoke(keyword)

        event.deferReply().setContent("Successfully filtered emoji with name! : $keyword").setEphemeral(true).queue()
    }
}