package mandarin.card.supporter.holder.modal.slot

import common.CommonStatic
import mandarin.card.supporter.slot.SlotEntryFee
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.reflect.KMutableProperty0

class SlotManualRollInputHolder(author: Message, channelID: String, message: Message, private val input: KMutableProperty0<Long>, private val slotMachine: SlotMachine) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
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

        val entryEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        if (entryFee < slotMachine.entryFee.minimumFee) {
            event.deferReply().setContent("You must put at least $entryEmoji ${slotMachine.entryFee.minimumFee}!").setEphemeral(true).queue()

            return
        }

        if (entryFee > slotMachine.entryFee.maximumFee) {
            event.deferReply().setContent("You can put only up to $entryEmoji ${slotMachine.entryFee.maximumFee}!").setEphemeral(true).queue()

            return
        }

        input.set(entryFee)

        event.deferReply()
            .setContent("Successfully set the input to $entryEmoji$entryFee!")
            .setEphemeral(true)
            .queue()

        goBack()
    }

    override fun clean() {

    }
}