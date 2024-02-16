package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class SalvageCostModifyHolder(author: Message, channelID: String, messageID: String, private val mode: CardData.SalvageMode, private val editor: Runnable) : ModalHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "salvageCost")
            return

        val value = getValueFromMap(event.values, "cost")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Cost value must be numeric!")
                .setEphemeral(true)
                .queue()

            return
        }

        val cost = value.toDouble().toInt()

        if (cost <= 0) {
            event.deferReply()
                .setContent("Cost value must be positive!")
                .setEphemeral(true)
                .queue()

            return
        }

        val oldCost = mode.cost

        mode.cost = cost

        val cardType = when(mode) {
            CardData.SalvageMode.T1 -> "Tier 1 [Common]"
            CardData.SalvageMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.SalvageMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.SalvageMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.SalvageMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.SalvageMode.T4 -> "Tier 4 [Legend Rare]"
        }

        event.deferReply()
            .setContent("Successfully changed salvage cost of $cardType cards! Check applied result above")
            .setEphemeral(true)
            .queue()

        editor.run()

        TransactionLogger.logSalvageCostModified(authorMessage.author.id, mode, oldCost, cost)
    }
}