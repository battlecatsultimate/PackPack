package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CatFoodRateHolder
import mandarin.card.supporter.holder.modal.CooldownRateHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class CatFoodRateConfigHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "cf" -> {
                val min = TextInput.create("min", "Minimum Cat Food", TextInputStyle.SHORT)
                    .setPlaceholder("Define minimum amount of cat foods that will be given to user while chatting")
                    .setValue(CardData.minimumCatFoods.toString())
                    .setRequired(true)
                    .build()

                val max = TextInput.create("max", "Maximum Cat Food", TextInputStyle.SHORT)
                    .setPlaceholder("Define maximum amount of cat foods that will be given to user while chatting")
                    .setValue(CardData.maximumCatFoods.toString())
                    .setRequired(true)
                    .build()

                val modal = Modal.create("cf", "Cat Food Rate")
                    .addActionRow(min)
                    .addActionRow(max)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CatFoodRateHolder(authorMessage, channelID, message.id, this::applyResult))
            }
            "cooldown" -> {
                val cooldown = TextInput.create("cooldown", "Cooldown (In Seconds)", TextInputStyle.SHORT)
                    .setPlaceholder("Cooldown until new cat food will be given to user")
                    .setValue((CardData.catFoodCooldown / 1000).toString())
                    .setRequired(true)
                    .build()

                val modal = Modal.create("cooldown", "Cooldown")
                    .addActionRow(cooldown)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CooldownRateHolder(authorMessage, channelID, message.id, this::applyResult))
            }
            "confirm" -> {
                CardBot.saveCardData()

                event.deferEdit().setContent("Confirmed config")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    private fun applyResult() {
        val timeText = if (CardData.catFoodCooldown > 0)
            "Every `" + CardData.convertMillisecondsToText(CardData.catFoodCooldown) + "`"
        else
            "Every message"

        message.editMessage("Minimum Cat Food : ${EmojiStore.ABILITY["CF"]?.formatted} ${CardData.minimumCatFoods}\n" +
                "Maximum Cat Food : ${EmojiStore.ABILITY["CF"]?.formatted} ${CardData.maximumCatFoods}\n" +
                "\n" +
                "Cooldown : $timeText")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("cf", "Define Cat Foods").withEmoji(EmojiStore.ABILITY["CF"])))
        result.add(ActionRow.of(Button.secondary("cooldown", "Define Cooldown (in seconds)").withEmoji(Emoji.fromUnicode("⏰"))))
        result.add(ActionRow.of(Button.primary("confirm", "Confirm").withEmoji(Emoji.fromUnicode("✅"))))

        return result
    }
}