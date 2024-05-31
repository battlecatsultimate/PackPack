package mandarin.card.supporter.holder.modal

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineEntryFeeMinMaxHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine, private val isMinimum: Boolean) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "minMax")
            return

        val value = getValueFromMap(event.values, "entryFee")

        val entryFee = if (StaticStore.isNumeric(value)) {
            val entryFee = value.toLong()

            if (entryFee < 0) {
                event.deferReply().setContent("Entry fee must be positive!").setEphemeral(true).queue()

                return
            }

            entryFee
        } else {
            val suffix = when {
                value.endsWith("k") -> "k"
                value.endsWith("m") -> "m"
                value.endsWith("b") -> "b"
                else -> ""
            }

            if (suffix.isEmpty()) {
                event.deferReply().setContent("Entry fee must be numeric value!").setEphemeral(true).queue()

                return
            }

            val filteredValue = value.replace(suffix, "")

            if (!StaticStore.isNumeric(filteredValue)) {
                event.deferReply().setContent("Entry fee must be numeric value!").setEphemeral(true).queue()

                return
            }

            val multiplier = when(suffix) {
                "k" -> 1000
                "m" -> 1000000
                "b" -> 1000000000
                else -> 1
            }

            val entryFee = (filteredValue.toDouble() * multiplier).toLong()

            if (entryFee < 0) {
                event.deferReply().setContent("Entry fee must be positive!").setEphemeral(true).queue()

                return
            }

            entryFee
        }

        if (isMinimum) {
            if (entryFee > slotMachine.entryFee.maximumFee) {
                event.deferReply().setContent("Minimum entry fee must not exceed value of maximum entry fee!").setEphemeral(true).queue()

                return
            }

            slotMachine.entryFee.minimumFee = entryFee

            event.deferReply().setContent("Changed minimum entry fee like above! Check the result").setEphemeral(true).queue()
        } else {
            if (entryFee < slotMachine.entryFee.minimumFee) {
                event.deferReply().setContent("Maximum entry fee must not be lower than minimum entry fee!").setEphemeral(true).queue()

                return
            }

            slotMachine.entryFee.maximumFee = entryFee

            event.deferReply().setContent("Changed maximum entry fee like above! Check the result").setEphemeral(true).queue()
        }

        if (slotMachine in CardData.slotMachines) {
            CardBot.saveCardData()
        }

        goBack()
    }
}