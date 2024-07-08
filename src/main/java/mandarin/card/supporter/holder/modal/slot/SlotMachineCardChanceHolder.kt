package mandarin.card.supporter.holder.modal.slot

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.text.toDouble

class SlotMachineCardChanceHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine, private val cardChancePair: CardChancePair) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "chance")
            return

        val value = getValueFromMap(event.values, "chance")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Amount value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = value.toDouble()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        cardChancePair.chance = amount

        if (slotMachine in CardData.slotMachines) {
            CardBot.saveCardData()
        }

        event.deferReply()
            .setContent("Successfully changed chance of card/chance pair! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}