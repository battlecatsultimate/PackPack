package mandarin.card.supporter.holder.modal.slot

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.collections.forEach
import kotlin.math.min
import kotlin.text.toDouble

class SlotMachineSlotSizeModalHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "size")
            return

        val value = getValueFromMap(event.values, "slot")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Slot size value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Slot size value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        if (amount > 15) {
            event.deferReply().setContent("Max slot size is 15!").setEphemeral(true).queue()

            return
        }

        slotMachine.slotSize = amount

        slotMachine.content.forEach { c ->
            c.slot = min(c.slot, amount)
        }

        if (slotMachine in CardData.slotMachines) {
            CardBot.saveCardData()
        }

        event.deferReply()
            .setContent("Successfully set slot size for this slot machine! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}