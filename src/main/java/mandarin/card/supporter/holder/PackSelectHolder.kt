package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup
import mandarin.card.supporter.transaction.TransactionLogger
import mandarin.card.supporter.transaction.TransactionQueue
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.random.Random

class PackSelectHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                expired = true

                expire(authorMessage.author.id)

                val pack = when(event.values[0]) {
                    "large" -> CardData.Pack.LARGE
                    "small" -> CardData.Pack.SMALL
                    else -> CardData.Pack.NONE
                }

                val index = if (pack == CardData.Pack.SMALL)
                    0
                else
                    1

                if (CardData.cooldown.containsKey(authorMessage.author.id) && (CardData.cooldown[authorMessage.author.id]?.get(index) ?: -1) - System.currentTimeMillis() > 0) {
                    var leftTime = (CardData.cooldown[authorMessage.author.id]?.get(index) ?: -1) - System.currentTimeMillis()

                    val day = leftTime / (24 * 60 * 60 * 1000)

                    leftTime -= day * 24 * 60 * 60 * 1000

                    val hour = leftTime / (60 * 60 * 1000)

                    leftTime -= hour * 60 * 60 * 1000

                    val minute = leftTime / (60 * 1000)

                    leftTime -= minute * 60 * 1000

                    val second = leftTime / 1000

                    leftTime -= second * 1000

                    val secondTime = second + leftTime / 1000.0

                    val timeText = (if (day == 0L) "" else if (day == 1L) "$day day " else "$day days ") +
                            (if (hour == 0L) "" else if (hour == 1L) "$hour hour " else "$hour hours ") +
                            (if (minute == 0L) "" else if (minute == 1L) "$minute minute " else "$minute minutes ") +
                            (if (secondTime == 0.0) "" else if (secondTime <= 1.0) "$secondTime second " else "$secondTime seconds ")

                    event.deferEdit()
                        .setContent("You can't roll this pack because your cooldown didn't end yet. You have to wait for $timeText")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                if (!TatsuHandler.canInteract(1, false)) {
                    event.deferEdit()
                        .setContent("Sorry, bot is cleaning up queued cat food transactions, please try again later. Expected waiting time is approximately ${TransactionGroup.groupQueue.size + 1} minute(s)")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                val guild = event.guild ?: return

                val currentCatFood = TatsuHandler.getPoints(guild.idLong, authorMessage.author.idLong, false)

                if (currentCatFood - pack.cost < 0) {
                    event.deferEdit()
                        .setContent("You can't buy this pack because you have $currentCatFood cat foods, and pack's cost is ${pack.cost} cat foods")
                        .mentionRepliedUser(false)
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                } else if (currentCatFood - TradingSession.accumulateSuggestedCatFood(authorMessage.author.idLong) - pack.cost < 0) {
                    event.deferEdit()
                        .setContent("It seems you suggested cat foods in other trading sessions, so you can use your cat foods up to ${currentCatFood - TradingSession.accumulateSuggestedCatFood(authorMessage.author.idLong)} cat foods")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                if (TatsuHandler.canInteract(1, false)) {
                    event.deferEdit()
                        .setContent("\uD83C\uDFB2 Rolling...!")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    if (pack.cost > 0) {
                        TatsuHandler.modifyPoints(guild.idLong, authorMessage.author.idLong, pack.cost, TatsuHandler.Action.REMOVE, true)
                    }

                    val result = rollCards(pack)

                    val inventory = Inventory.getInventory(authorMessage.author.id)

                    inventory.addCards(result)

                    val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                    for (card in result) {
                        builder.append("- ").append(card.cardInfo()).append("\n")
                    }

                    Command.replyToMessageSafely(event.messageChannel, builder.toString(), authorMessage) { a ->
                        return@replyToMessageSafely a.addFiles(result.map { c ->
                            FileUpload.fromData(
                                c.cardImage,
                                "${c.name}.png"
                            )
                        })
                    }

                    val member = event.member ?: return

                    TransactionLogger.logRoll(result, pack, member, false)
                } else {
                    event.deferEdit()
                        .setContent("Your roll got queued. Please wait, and it will mention you when roll is done")
                        .queue()

                    TransactionGroup.queue(TransactionQueue(1) {
                        if (pack.cost > 0) {
                            TatsuHandler.modifyPoints(guild.idLong, authorMessage.author.idLong, pack.cost, TatsuHandler.Action.REMOVE, true)
                        }

                        val result = rollCards(pack)

                        val inventory = Inventory.getInventory(authorMessage.author.id)

                        inventory.addCards(result)

                        val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                        for (card in result) {
                            builder.append("- ").append(card.cardInfo()).append("\n")
                        }

                        event.messageChannel
                            .sendMessage(builder.toString())
                            .setMessageReference(authorMessage)
                            .addFiles(result.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                            .queue()

                        val member = event.member ?: return@TransactionQueue

                        TransactionLogger.logRoll(result, pack, member, false)
                    })
                }
            }
        }
    }

    private fun rollCards(pack: CardData.Pack) : List<Card> {
        val result = ArrayList<Card>()

        when(pack) {
            CardData.Pack.SMALL -> {
                repeat(4) {
                    result.add(CardData.common.random())
                }

                val chance = Random.nextDouble()

                if (chance <= 0.95) {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                } else {
                    result.add(CardData.ultraRare.random())
                }

                val nextTime = System.currentTimeMillis() + CardData.cooldownTerm

                if (CardData.cooldown.containsKey(authorMessage.author.id)) {
                    val cooldown = CardData.cooldown[authorMessage.author.id]

                    if (cooldown == null) {
                        CardData.cooldown[authorMessage.author.id] = longArrayOf(nextTime, -1)
                    } else {
                        cooldown[0] = nextTime
                    }
                } else {
                    CardData.cooldown[authorMessage.author.id] = longArrayOf(nextTime, -1)
                }
            }
            CardData.Pack.LARGE -> {
                repeat(8) {
                    result.add(CardData.common.random())
                }

                result.add(CardData.appendUncommon(CardData.uncommon).random())

                val chance = Random.nextDouble()

                if (chance <= 0.9) {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                } else if (chance <= 0.99) {
                    result.add(CardData.ultraRare.random())
                } else {
                    result.add(CardData.appendLR(CardData.legendRare).random())
                }

                val nextTime = System.currentTimeMillis() + CardData.cooldownTerm

                if (CardData.cooldown.containsKey(authorMessage.author.id)) {
                    val cooldown = CardData.cooldown[authorMessage.author.id]

                    if (cooldown == null) {
                        CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, nextTime)
                    } else {
                        cooldown[1] = nextTime
                    }
                } else {
                    CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, nextTime)
                }
            }
            else -> {
                throw IllegalStateException("Invalid pack type $pack found")
            }
        }

        return result
    }
}