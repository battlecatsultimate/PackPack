package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.math.max
import kotlin.math.min

class CatFoodRateHolder(author: Message, channelID: String, messageID: String, private val editor: Runnable) : ModalHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cf")
            return

        val min = getValueFromMap(event.values, "min").lowercase()
        val max = getValueFromMap(event.values, "max").lowercase()

        if (!StaticStore.isNumeric(min)) {
            event.reply("You must pass numeric values for minimum cat foods!")
                .setEphemeral(true)
                .queue()

            return
        }

        if (!StaticStore.isNumeric(max)) {
            event.reply("You must pass numeric values for maximum cat foods!")
                .setEphemeral(true)
                .queue()

            return
        }

        val minValue = min.toDouble().toLong()
        val maxValue = max.toDouble().toLong()

        if (minValue < 0) {
            event.reply("Minimum cat food value must be positive value!")
                .setEphemeral(true)
                .queue()

            return
        }

        if (maxValue < 0) {
            event.reply("Maximum cat food value must be positive value!")
                .setEphemeral(true)
                .queue()

            return
        }

        val oldRate = arrayOf(CardData.minimumCatFoods, CardData.maximumCatFoods)

        CardData.maximumCatFoods = max(maxValue, minValue)
        CardData.minimumCatFoods = min(maxValue, minValue)

        val newRate = arrayOf(CardData.minimumCatFoods, CardData.maximumCatFoods)

        event.reply("Successfully configured cat food rate!")
            .setEphemeral(true)
            .queue()

        editor.run()

        TransactionLogger.logCatFoodRateChange(authorMessage.author.id, oldRate.toLongArray(), newRate.toLongArray())
    }
}