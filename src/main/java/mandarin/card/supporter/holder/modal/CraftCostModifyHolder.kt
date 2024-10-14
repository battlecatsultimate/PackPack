package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CraftCostModifyHolder(author: Message, userID: String, channelID: String, message: Message, private val mode: CardData.CraftMode, private val editor: Runnable) : ModalHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "craftCost")
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
            CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
        }

        event.deferReply()
            .setContent("Successfully changed craft cost of $cardType cards! Check applied result above")
            .setEphemeral(true)
            .queue()

        editor.run()

        TransactionLogger.logSalvageCostModified(authorMessage.author.id, mode, oldCost, cost)
    }
}