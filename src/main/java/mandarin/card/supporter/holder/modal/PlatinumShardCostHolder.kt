package mandarin.card.supporter.holder.modal

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class PlatinumShardCostHolder(author: Message, channelID: String, message: Message, private val pack: CardPack) : ModalHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "shard")
            return

        val value = getValueFromMap(event.values, "amount")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Amount value must be numeric!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        val amount = StaticStore.safeParseLong(value)

        if (amount < 0) {
            event.deferReply()
                .setContent("Amount value must be 0 or positive!")
                .setEphemeral(true)
                .queue()

            goBack()

            return
        }

        pack.cost.platinumShards = amount

        if (pack in CardData.cardPacks) {
            CardBot.saveCardData()
        }

        event.deferReply()
            .setContent("Successfully set cost of platinum shards for this card pack! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}