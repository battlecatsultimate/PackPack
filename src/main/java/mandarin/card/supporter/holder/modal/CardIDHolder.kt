package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.io.File
import kotlin.math.abs

class CardIDHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card, private val createMode: Boolean) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cardID") {
            return
        }

        val value = getValueFromMap(event.values, "id")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("ID must be numeric!")
                .setEphemeral(true)
                .queue()

            return
        }

        val id = StaticStore.safeParseInt(value)

        if (card.tier != CardData.Tier.SPECIAL && id < 0) {
            event.deferReply()
                .setContent("Card ID must be positive if card tier isn't T0!")
                .setEphemeral(true)
                .queue()

            return
        }

        if (card.tier == CardData.Tier.SPECIAL && CardData.cards.filter { c -> c !== card && c.tier == CardData.Tier.SPECIAL }.any { c -> abs(c.id) == abs(id) }) {
            event.deferReply()
                .setContent("There's already a card with ID of ${-abs(id)}!")
                .setEphemeral(true)
                .queue()
        } else if (card.tier != CardData.Tier.SPECIAL && CardData.cards.filter { c -> c !== card && c.tier != CardData.Tier.SPECIAL }.any { c -> c.id == card.id }) {
            event.deferReply()
                .setContent("There's already a card with ID of $id!")
                .setEphemeral(true)
                .queue()
        }

        val previousID = card.id

        card.id = if (card.tier == CardData.Tier.SPECIAL) {
            -abs(id)
        } else {
            id
        }

        if (!createMode && previousID != card.id) {
            val tierFolder = when(card.tier) {
                CardData.Tier.SPECIAL -> "Tier 0"
                CardData.Tier.COMMON -> "Tier 1"
                CardData.Tier.UNCOMMON -> "Tier 2"
                CardData.Tier.ULTRA -> "Tier 3"
                CardData.Tier.LEGEND -> "Tier 4"
                CardData.Tier.NONE -> throw IllegalStateException("E/CardIDHolder::onEvent - Invalid tier ${card.tier} is assigned")
            }

            val cardFileName = "${abs(card.id)}-${card.name}"

            val newPlace = File("./data/cards/$tierFolder/$cardFileName.png")

            if (!card.cardImage.renameTo(newPlace)) {
                event.deferReply()
                    .setContent("Failed to move image file to proper place...")
                    .setEphemeral(true)
                    .queue()

                card.id = previousID

                return
            }
        }

        event.deferReply()
            .setContent("Successfully changed the card ID!")
            .setEphemeral(true)
            .queue()

        goBack()
    }

    override fun clean() {

    }
}