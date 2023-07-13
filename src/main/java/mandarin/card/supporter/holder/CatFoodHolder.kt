package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Suggestion
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CatFoodHolder(author: Message, channelID: String, message:Message, private val suggestMessage: Message, private val suggestion: Suggestion) : ModalHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        val value = getValueFromMap(event.values, "cf").lowercase()

        val member = authorMessage.member ?: return

        if (StaticStore.isNumeric(value)) {
            val catFood = value.toInt()

            if (catFood > 500000) {
                event.reply("You can't suggest cat foods more than 500k! Please contact moderator for such large transaction").setEphemeral(true).queue()

                return
            }

            if (!TatsuHandler.canInteract(1, false)) {
                event.reply("Sorry, bot is cleaning up queued cat food transaction queue, please try again later. Expected waiting time is approximately ${TransactionGroup.groupQueue.size + 1} minute(s)")
                    .setEphemeral(true)
                    .queue()

                return
            }

            val guild = event.guild ?: return

            val currentCatFood = TatsuHandler.getPoints(guild.idLong, member.idLong, false)

            if (currentCatFood - catFood < 0) {
                event.reply("The suggested amount of cat food ($catFood) is larger than what you have currently ($currentCatFood)!")
                    .setEphemeral(true)
                    .queue()

                return
            } else if (currentCatFood - catFood - TradingSession.accumulateSuggestedCatFood(member.idLong) < 0) {
                event.reply("It seems you already suggested other cat food in different trading session. You can suggest up to ${currentCatFood - TradingSession.accumulateSuggestedCatFood(member.idLong)} cat foods in this session!")
                    .setEphemeral(true)
                    .queue()

                return
            }

            suggestion.catFood = value.toInt()

            val tax = (suggestion.catFood * CardData.TAX).toInt()
            val actualCf = suggestion.catFood - tax

            event.reply("Successfully suggested said amount of cat food, please check above. Actual amount of cf you will get is $actualCf, tax being $tax").setEphemeral(true).queue()

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

                event.reply("Successfully suggested said amount of cat food, please check above. Actual amount of cf other trader will get is $actualCf, tax being $tax").setEphemeral(true).queue()

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