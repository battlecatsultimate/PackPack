package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Suggestion
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CatFoodHolder(author: Message, channelID: String, message:Message, private val suggestMessage: Message, private val suggestion: Suggestion) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        val value = getValueFromMap(event.values, "cf").lowercase()

        val member = authorMessage.member ?: return

        if (StaticStore.isNumeric(value)) {
            val catFood = value.toDouble().toInt()

            if (catFood <= 0) {
                event.reply("Cat food must be larger than 0!").setEphemeral(true).queue()

                return
            }

            val cf = EmojiStore.ABILITY["CF"]?.formatted

            if (catFood > CardData.MAX_CAT_FOOD_SUGGESTION) {
                event.reply("You can suggest only up to $cf ${CardData.MAX_CAT_FOOD_SUGGESTION}!")
                    .setEphemeral(true)
                    .queue()
            }

            val inventory = Inventory.getInventory(authorMessage.author.idLong)

            val currentCatFood = inventory.actualCatFood

            if (currentCatFood - catFood < 0) {
                event.reply("The suggested amount of cat food ($cf $catFood) is larger than what you can actually suggest ($cf $currentCatFood!")
                    .setEphemeral(true)
                    .queue()

                return
            }

            suggestion.catFood = catFood

            val tax = (suggestion.catFood * CardData.TAX).toInt()
            val actualCf = suggestion.catFood - tax

            if (tax == 0) {
                event.reply("Successfully suggested said amount of cat food, please check above").setEphemeral(true).queue()
            } else {
                event.reply("Successfully suggested said amount of cat food, please check above. Actual amount of cf you will get is $actualCf, tax being $tax").setEphemeral(true).queue()
            }

            suggestMessage
                .editMessage(suggestion.suggestionInfo(member))
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
                .queue()
        } else {
            val filtered = value.replace(Regex("[km]"), "")

            if (StaticStore.isNumeric(filtered)) {
                val multiplier = if (value.endsWith("k")) {
                    1000
                } else if (value.endsWith("m")) {
                    1000000
                } else {
                    1
                }

                val catFood = (filtered.toDouble() * multiplier).toInt()

                if (catFood > 500000) {
                    event.reply("You can't suggest cat foods more than 500k! Please contact moderator for such large transaction").setEphemeral(true).queue()

                    return
                }

                suggestion.catFood = catFood

                val tax = (suggestion.catFood * CardData.TAX).toInt()
                val actualCf = suggestion.catFood - tax

                if (tax == 0) {
                    event.reply("Successfully suggested said amount of cat food, please check above").setEphemeral(true).queue()
                } else {
                    event.reply("Successfully suggested said amount of cat food, please check above. Actual amount of cf other trader will get is $actualCf, tax being $tax").setEphemeral(true).queue()
                }

                suggestMessage
                    .editMessage(suggestion.suggestionInfo(member))
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()
            } else {
                event.reply("Please pass number only, please check example below :\n\n- 100\n- 10k\n- 1m").setEphemeral(true).queue()
            }
        }
    }
}