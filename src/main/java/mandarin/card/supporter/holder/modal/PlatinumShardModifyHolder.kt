package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.math.max
import kotlin.math.min

class PlatinumShardModifyHolder(author: Message, channelID: String, messageID: String, private val inventory: Inventory, private val isAdd: Boolean, private val targetMember: String) : ModalHolder(author, channelID, messageID) {
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

            return
        }

        val amount = value.toDouble().toLong()

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            return
        }

        if (isAdd) {
            val oldAmount = inventory.platinumShard

            event.deferReply()
                .setContent("Successfully added ${EmojiStore.ABILITY["SHARD"]?.formatted} $amount\n" +
                        "\n" +
                        "Result : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard} -> ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard + amount}")
                .mentionRepliedUser(false)
                .queue()

            inventory.platinumShard += amount

            val newAmount = inventory.platinumShard

            TransactionLogger.logPlatinumShardModification(authorMessage.author.id, targetMember, amount, true, oldAmount, newAmount)
        } else {
            val oldAmount = inventory.platinumShard
            val realAmount = min(amount, inventory.platinumShard)

            event.deferReply()
                .setContent("Successfully removed ${EmojiStore.ABILITY["SHARD"]?.formatted} $amount\n" +
                        "\n" +
                        "Result : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard} -> ${EmojiStore.ABILITY["SHARD"]?.formatted} ${max(0, inventory.platinumShard - amount)}")
                .mentionRepliedUser(false)
                .queue()

            inventory.platinumShard -= realAmount

            val newAmount = inventory.platinumShard

            TransactionLogger.logPlatinumShardModification(authorMessage.author.id, targetMember, realAmount, false, oldAmount, newAmount)
        }

        goBack()
    }
}