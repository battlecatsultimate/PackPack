package mandarin.card.supporter.holder.modal.auction

import common.CommonStatic
import mandarin.card.supporter.AuctionSession
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.text.endsWith
import kotlin.text.isEmpty
import kotlin.text.replace
import kotlin.text.toDouble
import kotlin.text.toLong

class AuctionBidHolder(author: Message, channelID: String, message: Message, private val auctionSession: AuctionSession, private val onSelected: (Long) -> Unit) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "bid")
            return

        val u = authorMessage.author

        val value = getValueFromMap(event.values, "bid")
        val inventory = Inventory.getInventory(u.idLong)

        if (StaticStore.isNumeric(value)) {
            val bid = value.toLong()

            if (bid < 0) {
                event.deferReply().setContent("Bid must be positive!").setEphemeral(true).queue()

                goBack()

                return
            }

            if (bid < auctionSession.currentBid + auctionSession.minimumBid) {
                event.deferReply().setContent("You have to bid at least ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.currentBid + auctionSession.minimumBid}!").setEphemeral(true).queue()

                goBack()

                return
            }

            if (bid > inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)) {
                event.deferReply().setContent("You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)}. You can't bid more than what you have!").setEphemeral(true).queue()

                goBack()

                return
            }

            onSelected.invoke(bid)

            event.deferReply().setContent("Please confirm your bid!").setEphemeral(true).queue()
        } else {
            val suffix = when {
                value.endsWith("k") -> "k"
                value.endsWith("m") -> "m"
                value.endsWith("b") -> "b"
                else -> ""
            }

            if (suffix.isEmpty()) {
                event.deferReply().setContent("Bid must be numeric value!").setEphemeral(true).queue()

                goBack()

                return
            }

            val filteredValue = value.replace(suffix, "")

            if (!StaticStore.isNumeric(filteredValue)) {
                event.deferReply().setContent("Bid must be numeric value!").setEphemeral(true).queue()

                goBack()

                return
            }

            val multiplier = when (suffix) {
                "k" -> 1000
                "m" -> 1000000
                "b" -> 1000000000
                else -> 1
            }

            val bid = (filteredValue.toDouble() * multiplier).toLong()

            if (bid < 0) {
                event.deferReply().setContent("Bid must be positive!").setEphemeral(true).queue()

                goBack()

                return
            }

            if (bid < auctionSession.currentBid + auctionSession.minimumBid) {
                event.deferReply().setContent("You have to bid at least ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.currentBid + auctionSession.minimumBid}!").setEphemeral(true).queue()

                goBack()

                return
            }

            if (bid > inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)) {
                event.deferReply().setContent("You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)}. You can't bid more than what you have!").setEphemeral(true).queue()

                goBack()

                return
            }

            onSelected.invoke(bid)

            event.deferReply().setContent("Please confirm your bid!").setEphemeral(true).queue()
        }
    }
}