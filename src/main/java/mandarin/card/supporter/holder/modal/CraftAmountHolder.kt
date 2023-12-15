package mandarin.card.supporter.holder.modal

import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CraftAmountHolder(author: Message, channelID: String, messageID: String, private val editor: (Int) -> Unit) : ModalHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "amount")
            return

        val value = getValueFromMap(event.values, "amount")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("The amount of card must be number!")
                .setEphemeral(true)
                .queue()

            return
        }

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply()
                .setContent("The amount of card must be positive!")
                .setEphemeral(true)
                .queue()

            return
        } else if (amount > 20) {
            event.deferReply()
                .setContent("Sorry! Max amount of card is up to 20!")
                .setEphemeral(true)
                .queue()

            return
        }

        event.deferReply()
            .setContent("Successfully changed amount of card that will be crafted! Check required amount of shards as well")
            .setEphemeral(true)
            .queue()

        editor.invoke(amount)
    }
}