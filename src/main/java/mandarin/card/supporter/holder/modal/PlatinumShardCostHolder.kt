package mandarin.card.supporter.holder.modal

import common.CommonStatic
import mandarin.card.supporter.pack.PackCost
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class PlatinumShardCostHolder(author: Message, channelID: String, message: Message, private val cost: PackCost) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
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

        cost.platinumShards = amount

        event.deferReply()
            .setContent("Successfully set cost of platinum shards for this card pack! Check result above")
            .setEphemeral(true)
            .queue()

        goBack()
    }
}