package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CardChancePairAmountHolder(author: Message, userID: String, channelID: String, message: Message, private val cardChancePairList: CardChancePairList) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

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

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        cardChancePairList.amount = amount

        event.deferReply()
            .setContent("Successfully changed amount of required cards! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}