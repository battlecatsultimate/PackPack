package mandarin.card.supporter.holder.modal.slot

import mandarin.card.supporter.Inventory
import mandarin.card.supporter.slot.SlotEntryFee
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.text.endsWith
import kotlin.text.isEmpty
import kotlin.text.replace
import kotlin.text.toDouble

class SlotMachineRollModalHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine, private val inventory: Inventory, private val onSelected: (ModalInteractionEvent, Long) -> Unit) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "roll")
            return

        val value = getValueFromMap(event.values, "fee")

        val entryFee = if (StaticStore.isNumeric(value)) {
            val entryFee = StaticStore.safeParseLong(value)

            if (entryFee <= 0) {
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

            if (entryFee <= 0) {
                event.deferReply().setContent("Entry fee must be positive!").setEphemeral(true).queue()

                return
            }

            entryFee
        }

        val overPaid: Boolean
        val entryEmoji: String?
        val entryName : String
        val possibleAmount: Long

        when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> {
                overPaid = entryFee > inventory.actualCatFood
                entryEmoji = EmojiStore.ABILITY["CF"]?.formatted
                entryName = "Cat Foods"
                possibleAmount = inventory.actualCatFood
            }
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> {
                overPaid = entryFee > inventory.platinumShard
                entryEmoji = EmojiStore.ABILITY["SHARD"]?.formatted
                entryName = "Platinum Shards"
                possibleAmount = inventory.platinumShard
            }
        }

        if (overPaid) {
            event.deferReply().setContent("You have put too much $entryEmoji $entryName! You can put only up to $entryEmoji $possibleAmount!").setEphemeral(true).queue()

            return
        }

        if (entryFee < slotMachine.entryFee.minimumFee) {
            event.deferReply().setContent("You must put at least $entryEmoji ${slotMachine.entryFee.minimumFee}!").setEphemeral(true).queue()

            return
        }

        if (entryFee > slotMachine.entryFee.maximumFee) {
            event.deferReply().setContent("You can put only up to $entryEmoji ${slotMachine.entryFee.maximumFee}!").setEphemeral(true).queue()

            return
        }

        onSelected.invoke(event, entryFee)
    }
}