package mandarin.card.supporter.holder.modal.auction

import common.CommonStatic
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.text.endsWith
import kotlin.text.isEmpty
import kotlin.text.replace
import kotlin.text.toDouble
import kotlin.text.toLong

class AuctionMinimumBidHolder(author: Message, userID: String, channelID: String, message: Message, private val onSelected: (Long) -> Unit) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "minimumBid")
            return

        val value = getValueFromMap(event.values, "bid")

        if (StaticStore.isNumeric(value)) {
            val bid = value.toLong()

            if (bid < 0) {
                event.deferReply().setContent("Minimum bid increase must be positive!").setEphemeral(true).queue()

                return
            }

            onSelected.invoke(bid)

            event.deferReply().setContent("Successfully set minimum bid increase like above!").setEphemeral(true).queue()
        } else {
            val suffix = when {
                value.endsWith("k") -> "k"
                value.endsWith("m") -> "m"
                value.endsWith("b") -> "b"
                else -> ""
            }

            if (suffix.isEmpty()) {
                event.deferReply().setContent("Bid must be numeric value!").setEphemeral(true).queue()

                return
            }

            val filteredValue = value.replace(suffix, "")

            if (!StaticStore.isNumeric(filteredValue)) {
                event.deferReply().setContent("Bid must be numeric value!").setEphemeral(true).queue()

                return
            }

            val multiplier = when(suffix) {
                "k" -> 1000
                "m" -> 1000000
                "b" -> 1000000000
                else -> 1
            }

            val bid = (filteredValue.toDouble() * multiplier).toLong()

            if (bid < 0) {
                event.deferReply().setContent("Minimum bid increase must be positive!").setEphemeral(true).queue()

                return
            }

            onSelected.invoke(bid)

            event.deferReply().setContent("Successfully set minimum bid increase like above!").setEphemeral(true).queue()
        }
    }
}