package mandarin.card.supporter.holder.modal

import common.CommonStatic.Lang.Locale
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class BannerNameHolder(author: Message, userID: String, channelID: String, message: Message, private val banner: Banner) : ModalHolder(author, userID, channelID, message, Locale.EN) {
    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "bannerName") {
            return
        }

        val name = getValueFromMap(event.values, "name").trim()

        if (name.isBlank()) {
            event.deferReply().setContent("Name must not be blank!").setEphemeral(true).queue()

            return
        }

        if (CardData.banners.any { b -> b !== banner && b.name == name }) {
            event.deferReply().setContent("This name is already assigned by other banner!").setEphemeral(true).queue()

            return
        }

        banner.name = name

        event.deferReply()
            .setContent("Successfully changed the banner name!")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}