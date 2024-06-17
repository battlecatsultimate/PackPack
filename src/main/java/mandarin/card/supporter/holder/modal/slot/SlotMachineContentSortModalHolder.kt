package mandarin.card.supporter.holder.modal.slot

import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineContentSortModalHolder(author: Message, channelID: String, message: Message, private val onSelect: (Int, ModalInteractionEvent) -> Unit) : ModalHolder(author, channelID, message) {
    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "sort")
            return

        val value = getValueFromMap(event.values, "index")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply().setContent("You have to put only numeric values!").setEphemeral(true).queue()

            return
        }

        val amount = StaticStore.safeParseInt(value)

        if (amount <= 0) {
            event.deferReply().setContent("Value must be positive number!").setEphemeral(true).queue()

            return
        }

        onSelect.invoke(amount, event)
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }
}