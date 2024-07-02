package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.card.Card
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.math.min

class CardFavoriteAmountHolder(author: Message, channelID: String, message: Message, private val inventory: Inventory, private val card: Card, private val favorite: Boolean) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "favorite")
            return

        val value = getValueFromMap(event.values, "amount")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Amount value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        if (favorite)
            inventory.favoriteCards(card, min(amount, inventory.cards[card] ?: 0))
        else
            inventory.unfavoriteCards(card, min(amount, inventory.favorites[card] ?: 0))

        if (favorite) {
            event.deferReply()
                .setContent("Successfully added cards into favorite. Check the result above!")
                .setEphemeral(true)
                .queue()
        } else {
            event.deferReply()
                .setContent("Successfully removed cards from favorite. Check the result above!")
                .setEphemeral(true)
                .queue()
        }

        goBack()
    }
}