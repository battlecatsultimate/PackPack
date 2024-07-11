package mandarin.card.supporter.holder.modal.slot

import common.CommonStatic
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SlotMachineCardRewardNameModalHolder(author: Message, channelID: String, message: Message, private val content: SlotCardContent) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "name")
            return

        val value = getValueFromMap(event.values, "name")

        content.name = value

        event.deferReply().setContent("Successfully changed the name like above!").setEphemeral(true).queue()

        goBack()
    }
}