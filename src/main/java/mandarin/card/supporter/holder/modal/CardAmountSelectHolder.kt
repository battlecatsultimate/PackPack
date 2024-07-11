package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CardAmountSelectHolder(author: Message, channelID: String, message: Message, private val onSelect: (Int) -> Unit) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "select") {
            return
        }

        val value = getValueFromMap(event.values, "amount")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply().setContent("You have to put only numeric values!").setEphemeral(true).queue()

            return
        }

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply().setContent("Value must be positive number!").setEphemeral(true).queue()

            return
        }

        onSelect.invoke(amount)

        event.deferReply().setContent("Successfully applied the selected cards. Check the result above!").setEphemeral(true).queue()
    }
}