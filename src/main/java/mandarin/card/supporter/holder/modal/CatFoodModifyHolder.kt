package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.math.max

class CatFoodModifyHolder(author: Message, channelID: String, message: Message, private val inventory: Inventory, private val isAdd: Boolean, private val targetMember: String) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cf")
            return

        val value = getValueFromMap(event.values, "amount")

        if (!StaticStore.isNumeric(value)) {
            event.deferReply()
                .setContent("Amount value must be numeric!")
                .setEphemeral(true)
                .queue()

            return
        }

        val amount = StaticStore.safeParseLong(value)

        if (amount <= 0) {
            event.deferReply()
                .setContent("Amount value must be positive!")
                .setEphemeral(true)
                .queue()

            return
        }

        if (isAdd) {
            val oldAmount = inventory.catFoods

            event.deferReply()
                .setContent("Successfully added ${EmojiStore.ABILITY["CF"]?.formatted} $amount\n" +
                        "\n" +
                        "Result : ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods} -> ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods + amount}")
                .mentionRepliedUser(false)
                .queue()

            inventory.catFoods += amount

            val newAmount = inventory.catFoods

            TransactionLogger.logCatFoodModification(authorMessage.author.id, targetMember, amount, true, oldAmount, newAmount)
        } else {
            val oldAmount = inventory.catFoods

            event.deferReply()
                .setContent("Successfully removed ${EmojiStore.ABILITY["CF"]?.formatted} $amount\n" +
                        "\n" +
                        "Result : ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods} -> ${EmojiStore.ABILITY["CF"]?.formatted} ${max(0, inventory.catFoods - amount)}")
                .mentionRepliedUser(false)
                .queue()

            inventory.catFoods = max(0, inventory.catFoods - amount)

            val newAmount = inventory.catFoods

            TransactionLogger.logCatFoodModification(authorMessage.author.id, targetMember, amount, false, oldAmount, newAmount)
        }

        goBack()
    }
}