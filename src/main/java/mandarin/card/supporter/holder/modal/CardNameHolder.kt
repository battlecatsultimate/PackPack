package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import java.io.File
import kotlin.math.abs

class CardNameHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card, private val createMode: Boolean) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cardName") {
            return
        }

        val name = getValueFromMap(event.values, "name")

        if (name.isBlank()) {
            event.deferReply().setContent("Name must not be blank!").setEphemeral(true).queue()

            return
        }

        if (name.contains(Regex("[\\\\/:*?\"<>|]"))) {
            event.deferReply().setContent("Name shall not contain any letters of : `\\ / : * ? \" < > |`").setEphemeral(true).queue()

            return
        }

        val previousName = card.name

        card.name = name.trim()

        if (!createMode && previousName != card.name) {
            val tierFolder = when(card.tier) {
                CardData.Tier.SPECIAL -> "Tier 0"
                CardData.Tier.COMMON -> "Tier 1"
                CardData.Tier.UNCOMMON -> "Tier 2"
                CardData.Tier.ULTRA -> "Tier 3"
                CardData.Tier.LEGEND -> "Tier 4"
                CardData.Tier.NONE -> throw IllegalStateException("E/CardNameHolder::onEvent - Invalid tier ${card.tier} is assigned")
            }

            val cardFileName = "${abs(card.id)}-${card.name}"

            val newPlace = File("./data/cards/$tierFolder/$cardFileName.png")

            if (!card.cardImage.renameTo(newPlace)) {
                event.deferReply()
                    .setContent("Failed to move image file to proper place...")
                    .setEphemeral(true)
                    .queue()

                card.name = previousName

                return
            }
        }

        event.deferReply()
            .setContent("Successfully changed the card name!")
            .setEphemeral(true)
            .queue()

        goBack()
    }

    override fun clean() {

    }
}