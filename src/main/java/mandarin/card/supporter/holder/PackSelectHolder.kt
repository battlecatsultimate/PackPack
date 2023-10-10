package mandarin.card.supporter.holder

import mandarin.card.supporter.*
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.transaction.TransactionQueue
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.math.min
import kotlin.random.Random

class PackSelectHolder(author: Message, channelID: String, message: Message, private val noImage: Boolean) : ComponentHolder(author, channelID, message.id) {
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
                    "premium" -> CardData.Pack.PREMIUM
                    else -> CardData.Pack.NONE
                }

                val index = when (pack) {
                    CardData.Pack.SMALL -> CardData.SMALL
                    CardData.Pack.LARGE -> CardData.LARGE
                    else -> CardData.PREMIUM
                }

                if (CardData.cooldown.containsKey(authorMessage.author.id) && (CardData.cooldown[authorMessage.author.id]?.get(index) ?: -1) - CardData.getUnixEpochTime() > 0) {
                    val leftTime = (CardData.cooldown[authorMessage.author.id]?.get(index) ?: -1) - CardData.getUnixEpochTime()

                    event.deferEdit()
                        .setContent("You can't roll this pack because your cooldown didn't end yet. You have to wait for ${CardData.convertMillisecondsToText(leftTime)}")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                val guild = event.guild ?: return

                when (pack) {
                    CardData.Pack.SMALL,
                    CardData.Pack.LARGE -> {
                        if (!TatsuHandler.canInteract(1, false)) {
                            event.deferEdit()
                                .setContent("Sorry, bot is cleaning up queued cat food transactions, please try again later. Expected waiting time is approximately ${TransactionGroup.groupQueue.size + 1} minute(s)")
                                .setComponents()
                                .setAllowedMentions(ArrayList())
                                .mentionRepliedUser(false)
                                .queue()

                            return
                        }

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

                            try {
                                val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                                for (card in result) {
                                    builder.append("- ")

                                    if (card.tier == CardData.Tier.ULTRA) {
                                        builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
                                    } else if (card.tier == CardData.Tier.LEGEND) {
                                        builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
                                    }

                                    builder.append(card.cardInfo())

                                    if (!inventory.cards.containsKey(card)) {
                                        builder.append(" {**NEW**}")
                                    }

                                    if (card.tier == CardData.Tier.ULTRA) {
                                        builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
                                    } else if (card.tier == CardData.Tier.LEGEND) {
                                        builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                                    }

                                    builder.append("\n")
                                }

                                if (noImage) {
                                    event.messageChannel
                                        .sendMessage(builder.toString())
                                        .setMessageReference(authorMessage)
                                        .queue()
                                } else {
                                    event.messageChannel
                                        .sendMessage(builder.toString())
                                        .setMessageReference(authorMessage)
                                        .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                                        .queue()
                                }
                            } catch (e: Exception) {
                                StaticStore.logger.uploadErrorLog(e, "Failed to upload card roll message")
                            }

                            inventory.addCards(result)

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

                                try {
                                    val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                                    for (card in result) {
                                        builder.append("- ")

                                        if (card.tier == CardData.Tier.ULTRA) {
                                            builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
                                        } else if (card.tier == CardData.Tier.LEGEND) {
                                            builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
                                        }

                                        builder.append(card.cardInfo())

                                        if (!inventory.cards.containsKey(card)) {
                                            builder.append(" {**NEW**}")
                                        }

                                        if (card.tier == CardData.Tier.ULTRA) {
                                            builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
                                        } else if (card.tier == CardData.Tier.LEGEND) {
                                            builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                                        }

                                        builder.append("\n")
                                    }

                                    if (noImage) {
                                        event.messageChannel
                                            .sendMessage(builder.toString())
                                            .setMessageReference(authorMessage)
                                            .mentionRepliedUser(false)
                                            .queue()
                                    } else {
                                        event.messageChannel
                                            .sendMessage(builder.toString())
                                            .setMessageReference(authorMessage)
                                            .mentionRepliedUser(false)
                                            .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                                            .queue()
                                    }
                                } catch (e: Exception) {
                                    StaticStore.logger.uploadErrorLog(e, "Failed to upload card roll message")
                                }

                                inventory.addCards(result)

                                val member = event.member ?: return@TransactionQueue

                                TransactionLogger.logRoll(result, pack, member, false)
                            })
                        }
                    }
                    else -> {
                        val inventory = Inventory.getInventory(authorMessage.author.id)
                        val cards = inventory.cards.keys.filter { c -> c.tier == CardData.Tier.UNCOMMON }.sortedWith(
                            CardComparator()
                        )

                        if (cards.sumOf { inventory.cards[it] ?: 0 } < 5) {
                            event.deferEdit()
                                .setContent("It seems you can't afford this pack because you don't have 5 tier 2 [uncommon] cards")
                                .setComponents()
                                .setAllowedMentions(ArrayList())
                                .mentionRepliedUser(false)
                                .queue()

                            return
                        }

                        event.deferEdit()
                            .setContent(getPremiumText(cards, inventory))
                            .setComponents(assignComponents(cards))
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .queue()

                        expired = true
                        expire(authorMessage.author.id)

                        StaticStore.putHolder(authorMessage.author.id, CardSelectHolder(authorMessage, channelID, messageID) { _, e ->
                            e.deferEdit()
                                .setContent("\uD83C\uDFB2 Rolling...!")
                                .setComponents()
                                .setAllowedMentions(ArrayList())
                                .mentionRepliedUser(false)
                                .queue()

                            val result = rollCards(pack)

                            try {
                                val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                                for (card in result) {
                                    builder.append("- ")

                                    if (card.tier == CardData.Tier.ULTRA) {
                                        builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
                                    } else if (card.tier == CardData.Tier.LEGEND) {
                                        builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
                                    }

                                    builder.append(card.cardInfo())

                                    if (!inventory.cards.containsKey(card)) {
                                        builder.append(" {**NEW**}")
                                    }

                                    if (card.tier == CardData.Tier.ULTRA) {
                                        builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
                                    } else if (card.tier == CardData.Tier.LEGEND) {
                                        builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                                    }

                                    builder.append("\n")
                                }

                                if (noImage) {
                                    e.messageChannel
                                        .sendMessage(builder.toString())
                                        .setMessageReference(authorMessage)
                                        .mentionRepliedUser(false)
                                        .queue()
                                } else {
                                    e.messageChannel
                                        .sendMessage(builder.toString())
                                        .setMessageReference(authorMessage)
                                        .mentionRepliedUser(false)
                                        .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                                        .queue()
                                }
                            } catch (e: Exception) {
                                StaticStore.logger.uploadErrorLog(e, "Failed to upload card roll message")
                            }

                            inventory.addCards(result)

                            val member = e.member ?: return@CardSelectHolder

                            TransactionLogger.logRoll(result, pack, member, false)
                        })
                    }
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

                if (chance <= 0.7) {
                    result.add(CardData.common.random())
                } else {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                }

                val nextTime = CardData.getUnixEpochTime() + CardData.smallLargePackCooldown

                if (CardData.cooldown.containsKey(authorMessage.author.id)) {
                    val cooldown = CardData.cooldown[authorMessage.author.id]

                    if (cooldown == null) {
                        CardData.cooldown[authorMessage.author.id] = longArrayOf(nextTime, -1, -1)
                    } else {
                        cooldown[CardData.SMALL] = nextTime
                    }
                } else {
                    CardData.cooldown[authorMessage.author.id] = longArrayOf(nextTime, -1, -1)
                }
            }
            CardData.Pack.LARGE -> {
                repeat(8) {
                    result.add(CardData.common.random())
                }

                var chance = Random.nextDouble()

                if (chance <= 0.5) {
                    result.add(CardData.common.random())
                } else {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                }

                chance = Random.nextDouble()

                if (chance <= 0.99) {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                } else {
                    result.add(CardData.appendUltra(CardData.ultraRare).random())
                }

                val nextTime = CardData.getUnixEpochTime() + CardData.smallLargePackCooldown

                if (CardData.cooldown.containsKey(authorMessage.author.id)) {
                    val cooldown = CardData.cooldown[authorMessage.author.id]

                    if (cooldown == null) {
                        CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, nextTime, -1)
                    } else {
                        cooldown[CardData.LARGE] = nextTime
                    }
                } else {
                    CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, nextTime, -1)
                }
            }
            CardData.Pack.PREMIUM -> {
                repeat(5) {
                    val chance = Random.nextDouble()

                    if (chance <= 0.93) {
                        result.add(CardData.common.random())
                    } else if (chance <= 0.995) {
                        result.add(CardData.appendUltra(CardData.ultraRare).random())
                    } else {
                        result.add(CardData.appendLR(CardData.legendRare).random())
                    }
                }

                val nextTime = CardData.getUnixEpochTime() + CardData.premiumPackCooldown

                if (CardData.cooldown.containsKey(authorMessage.author.id)) {
                    val cooldown = CardData.cooldown[authorMessage.author.id]

                    if (cooldown == null) {
                        CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, -1, nextTime)
                    } else {
                        cooldown[CardData.PREMIUM] = nextTime
                    }
                } else {
                    CardData.cooldown[authorMessage.author.id] = longArrayOf(-1, -1, nextTime)
                }
            }
            else -> {
                throw IllegalStateException("Invalid pack type $pack found")
            }
        }

        return result
    }

    private fun assignComponents(cards: List<Card>) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        CardData.bannerCategoryText[CardData.Tier.UNCOMMON.ordinal].forEachIndexed { i, a ->
            bannerCategoryElements.add(SelectOption.of(a, "category-${CardData.Tier.UNCOMMON.ordinal}-$i"))
        }

        val bannerCategory = StringSelectMenu.create("category")
            .addOptions(bannerCategoryElements)
            .setPlaceholder("Filter Cards by Banners")

        rows.add(ActionRow.of(bannerCategory.build()))

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty())
            .build()

        rows.add(ActionRow.of(cardCategory))

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("select", "Select").asDisabled())
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getPremiumText(cards: List<Card>, inventory: Inventory) : String {
        val builder = StringBuilder("Select 5 Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n- No Cards Selected\n\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in 0 until min(SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = inventory.cards[cards[i]] ?: 1

                if (amount >= 2) {
                    builder.append(" x$amount\n")
                } else {
                    builder.append("\n")
                }
            }
        } else {
            builder.append("No Cards Found")
        }

        builder.append("```")

        return builder.toString()
    }
}