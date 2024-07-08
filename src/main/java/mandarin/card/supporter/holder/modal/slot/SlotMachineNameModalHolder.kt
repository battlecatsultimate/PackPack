package mandarin.card.supporter.holder.modal.slot

import mandarin.card.supporter.holder.slot.SlotMachineManageHolder
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineNameModalHolder : ModalHolder {
    private val slotMachine: SlotMachine?

    constructor(author: Message, channelID: String, message: Message) : super(author, channelID, message) {
        this.message = message

        slotMachine = null
    }

    constructor(author: Message, channelID: String, message: Message, slotMachine: SlotMachine) : super(author, channelID, message) {
        this.slotMachine = slotMachine
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        val p = parent ?: return

        if (event.modalId != "name")
            return

        val value = getValueFromMap(event.values, "name")

        if (slotMachine == null) {
            val slot = SlotMachine(value)

            p.connectTo(event, SlotMachineManageHolder(authorMessage, channelID, message, slot, true))
        } else {
            slotMachine.name = value

            event.deferReply().setContent("Successfully change name! Check above").setEphemeral(true).queue()

            goBack()
        }
    }
}