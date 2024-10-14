package mandarin.card.supporter.holder.modal.slot

import common.CommonStatic
import mandarin.card.supporter.holder.slot.SlotMachineManageHolder
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineNameModalHolder : ModalHolder {
    private val slotMachine: SlotMachine?

    constructor(author: Message, userID: String, channelID: String, message: Message) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.message = message

        slotMachine = null
    }

    constructor(author: Message, userID: String, channelID: String, message: Message, slotMachine: SlotMachine) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.slotMachine = slotMachine
    }

    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        val p = parent ?: return

        if (event.modalId != "name")
            return

        val value = getValueFromMap(event.values, "name")

        if (slotMachine == null) {
            val slot = SlotMachine(value)

            p.connectTo(event, SlotMachineManageHolder(authorMessage, userID, channelID, message, slot, true))
        } else {
            slotMachine.name = value

            event.deferReply().setContent("Successfully change name! Check above").setEphemeral(true).queue()

            goBack()
        }
    }
}