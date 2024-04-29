package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CooldownRateHolder(author: Message, channelID: String, messageID: String, private val editor: Runnable) : ModalHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cooldown")
            return

        val cooldown = getValueFromMap(event.values, "cooldown").lowercase()

        if (!StaticStore.isNumeric(cooldown)) {
            event.reply("You must pass numeric values for cooldown!")
                .setEphemeral(true)
                .queue()

            return
        }

        val cooldownValue = StaticStore.safeParseLong(cooldown)

        if (cooldownValue < 0) {
            event.reply("Cooldown must be positive value!")
                .setEphemeral(true)
                .queue()

            return
        }

        val oldValue = CardData.catFoodCooldown

        CardData.catFoodCooldown = cooldownValue * 1000L

        val newValue = CardData.catFoodCooldown

        event.reply("Successfully configured cooldown!")
            .setEphemeral(true)
            .queue()

        editor.run()

        TransactionLogger.logCatFoodCooldownChange(authorMessage.author.id, oldValue, newValue)
    }
}