package mandarin.card.supporter.holder.modal

import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class AuctionInitialPriceHolder(author: Message, channelID: String, message: Message, private val onSelected: (Long) -> Unit) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "price")
            return

        val value = getValueFromMap(event.values, "price")

        if (StaticStore.isNumeric(value)) {
            val price = value.toLong()

            if (price < 0) {
                event.deferReply().setContent("Initial price must be positive!").setEphemeral(true).queue()

                return
            }

            onSelected.invoke(price)

            event.deferReply().setContent("Successfully set initial price like above!").setEphemeral(true).queue()
        } else {
            val suffix = when {
                value.endsWith("k") -> "k"
                value.endsWith("m") -> "m"
                value.endsWith("b") -> "b"
                else -> ""
            }

            if (suffix.isEmpty()) {
                event.deferReply().setContent("Price must be numeric value!").setEphemeral(true).queue()

                return
            }

            val filteredValue = value.replace(suffix, "")

            if (!StaticStore.isNumeric(filteredValue)) {
                event.deferReply().setContent("Price must be numeric value!").setEphemeral(true).queue()

                return
            }

            val multiplier = when(suffix) {
                "k" -> 1000
                "m" -> 1000000
                "b" -> 1000000000
                else -> 1
            }

            val price = (filteredValue.toDouble() * multiplier).toLong()

            if (price < 0) {
                event.deferReply().setContent("Initial price must be positive!").setEphemeral(true).queue()

                return
            }

            onSelected.invoke(price)

            event.deferReply().setContent("Successfully set initial price like above!").setEphemeral(true).queue()
        }
    }
}