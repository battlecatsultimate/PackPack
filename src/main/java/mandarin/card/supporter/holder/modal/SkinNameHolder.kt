package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.card.Skin
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SkinNameHolder(author: Message, userID: String, channelID: String, message: Message, private val skin: Skin) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "skinName")
            return

        val name = getValueFromMap(event.values, "name")

        skin.name = name

        event.deferReply()
            .setContent("Successfully changed the name of the skin! Check the result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }

    override fun clean() {

    }
}