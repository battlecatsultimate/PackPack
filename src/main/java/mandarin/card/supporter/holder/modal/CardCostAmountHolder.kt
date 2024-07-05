package mandarin.card.supporter.holder.modal

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.CardCost
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CardCostAmountHolder(author: Message, channelID: String, message: Message, private val cost: CardCost) : ModalHolder(author, channelID, message) {
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
                .setContent("Amount value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = StaticStore.safeParseLong(value)

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        cost.amount = amount

        event.deferReply()
            .setContent("Successfully changed amount of required cards! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}