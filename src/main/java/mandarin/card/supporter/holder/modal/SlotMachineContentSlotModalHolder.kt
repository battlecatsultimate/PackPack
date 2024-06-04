package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.slot.SlotContent
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineContentSlotModalHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine, private val slotContent: SlotContent) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "slot")
            return

        val value = getValueFromMap(event.values, "size")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Slot value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = value.toDouble().toInt()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Slot value must be positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        if (amount > slotMachine.slotSize) {
            event.deferReply()
                .setContent("Slot value must not exceed slot machine's slot size : ${slotMachine.slotSize}")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        slotContent.slot = amount

        event.deferReply()
            .setContent("Successfully changed required amount of slot like above!")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}