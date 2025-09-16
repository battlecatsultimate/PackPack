package mandarin.card.supporter.holder.moderation

import common.CommonStatic
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
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal

class CatFoodRateConfigHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Cat food rate config expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "cf" -> {
                val min = TextInput.create("date.minute.uppercase.singular", TextInputStyle.SHORT)
                    .setPlaceholder("Define minimum amount of cat foods that will be given to user while chatting")
                    .setValue(CardData.minimumCatFoods.toString())
                    .setRequired(true)
                    .build()

                val max = TextInput.create("max", TextInputStyle.SHORT)
                    .setPlaceholder("Define maximum amount of cat foods that will be given to user while chatting")
                    .setValue(CardData.maximumCatFoods.toString())
                    .setRequired(true)
                    .build()

                val modal = Modal.create("cf", "Cat Food Rate")
                    .addComponents(Label.of("Minimum Cat Food", min))
                    .addComponents(Label.of("Maximum Cat Food", max))
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CatFoodRateHolder(authorMessage, userID, channelID, message, this::applyResult))
            }
            "cooldown" -> {
                val cooldown = TextInput.create("cooldown", TextInputStyle.SHORT)
                    .setPlaceholder("Cooldown until new cat food will be given to user")
                    .setValue((CardData.catFoodCooldown / 1000).toString())
                    .setRequired(true)
                    .build()

                val modal = Modal.create("cooldown", "Cooldown")
                    .addComponents(Label.of("Cooldown (In Seconds)", cooldown))
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CooldownRateHolder(authorMessage, userID, channelID, message, this::applyResult))
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

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(Button.secondary("cf", "Define Cat Foods").withEmoji(EmojiStore.ABILITY["CF"])))
        result.add(ActionRow.of(Button.secondary("cooldown", "Define Cooldown (in seconds)").withEmoji(Emoji.fromUnicode("⏰"))))
        result.add(ActionRow.of(Button.primary("confirm", "Confirm").withEmoji(Emoji.fromUnicode("✅"))))

        return result
    }
}