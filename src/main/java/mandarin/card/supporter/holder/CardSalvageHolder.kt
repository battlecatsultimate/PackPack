package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup
import mandarin.card.supporter.transaction.TransactionLogger
import mandarin.card.supporter.transaction.TransactionQueue
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
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

class CardSalvageHolder(author: Message, channelID: String, private val message: Message, private val salvageMode: Boolean) : ComponentHolder(author, channelID, message.id) {
    private val inventory = Inventory.getInventory(author.author.id)
    private val cards = ArrayList<Card>(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.COMMON }.sortedWith(CardComparator()))

    private val selectedCard = ArrayList<Card>()

    private var page = 0
    private var banner = intArrayOf(-1, -1)

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page -= 10

                applyResult(event)
            }
            "salvage" -> {
                val g = event.guild ?: return

                val cf = selectedCard.size * 300

                val cost = cf / 100000 + 1

                inventory.removeCards(selectedCard)

                if (TatsuHandler.canInteract(cost, false)) {
                    var c = cf

                    while (c > 0) {
                        val possible = min(c, 100000)

                        TatsuHandler.modifyPoints(g.idLong, authorMessage.author.idLong, possible, TatsuHandler.Action.ADD, true)

                        c -= possible
                    }

                    TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size)

                    event.deferEdit()
                            .setContent("Salvaged ${selectedCard.size} cards, and you received ${EmojiStore.ABILITY["CF"]?.formatted} ${selectedCard.size * 300}!")
                            .setComponents()
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .queue()
                } else {
                    TransactionGroup.queue(TransactionQueue(cost) {
                        event.deferEdit()
                                .setContent("You have been queued, job will be done after approximately ${TransactionGroup.groupQueue.size + 1} minute(s). Once job is done, bot will mention you")
                                .setComponents()
                                .mentionRepliedUser(false)
                                .setAllowedMentions(ArrayList())
                                .queue()

                        var c = cf

                        while (c > 0) {
                            val possible = min(c, 100000)

                            TatsuHandler.modifyPoints(g.idLong, authorMessage.author.idLong, possible, TatsuHandler.Action.ADD, true)

                            c -= possible
                        }

                        TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size)

                        event.messageChannel
                                .sendMessage("Salvaged ${selectedCard.size} cards, and you received ${EmojiStore.ABILITY["CF"]?.formatted} ${selectedCard.size * 300}!")
                                .setMessageReference(authorMessage)
                                .mentionRepliedUser(false)
                                .setAllowedMentions(ArrayList())
                                .queue()
                    })
                }

                CardBot.saveCardData()

                expired = true
                expire(authorMessage.author.id)
            }
            "craft" -> {
                val g = event.guild ?: return

                inventory.removeCards(selectedCard)

                val chance = Random.nextDouble()

                if (chance <= 0.7) {
                    val cf = selectedCard.size * 300

                    val cost = cf / 100000 + 1

                    if (TatsuHandler.canInteract(cost, false)) {
                        var c = cf

                        while (c > 0) {
                            val possible = min(c, 100000)

                            TatsuHandler.modifyPoints(g.idLong, authorMessage.author.idLong, possible, TatsuHandler.Action.ADD, true)

                            c -= possible
                        }

                        TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size)

                        event.deferEdit()
                                .setContent("Failed to craft tier 2 card..., so you received ${EmojiStore.ABILITY["CF"]?.formatted} ${selectedCard.size * 300}")
                                .setComponents()
                                .mentionRepliedUser(false)
                                .setAllowedMentions(ArrayList())
                                .queue()
                    } else {
                        TransactionGroup.queue(TransactionQueue(cost) {
                            event.deferEdit()
                                    .setContent("Failed to create tier 2 card..., ${EmojiStore.ABILITY["CF"]?.formatted} ${selectedCard.size * 300} will be received around ${TransactionGroup.groupQueue.size + 1} minute(s) later. Once job is done, bot will mention you")
                                    .setComponents()
                                    .mentionRepliedUser(false)
                                    .setAllowedMentions(ArrayList())
                                    .queue()

                            var c = cf

                            while (c > 0) {
                                val possible = min(c, 100000)

                                TatsuHandler.modifyPoints(g.idLong, authorMessage.author.idLong, possible, TatsuHandler.Action.ADD, true)

                                c -= possible
                            }

                            TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size)

                            event.messageChannel
                                    .sendMessage("You received ${EmojiStore.ABILITY["CF"]?.formatted} ${selectedCard.size * 300}")
                                    .setMessageReference(authorMessage)
                                    .mentionRepliedUser(false)
                                    .setAllowedMentions(ArrayList())
                                    .queue()
                        })
                    }
                } else {
                    val card = CardData.appendUncommon(CardData.uncommon).random()

                    inventory.addCards(listOf(card))

                    event.deferEdit()
                            .setContent("Successfully crafted tier 2 cards, and you obtained card below!\n\n${card.cardInfo()}")
                            .setFiles(FileUpload.fromData(card.cardImage, "card.png"))
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()
                }

                CardBot.saveCardData()

                expired = true
                expire(authorMessage.author.id)
            }
            "cancel" -> {
                expired = true

                event.deferEdit()
                        .setContent("Inventory Closed")
                        .setComponents(ArrayList())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                expire(authorMessage.author.id)
            }
            "category" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.size < 1)
                    return

                val value = event.values[0]

                banner = if (value == "all") {
                    intArrayOf(-1, -1)
                } else {
                    val data = value.split("-")

                    intArrayOf(data[1].toInt(), data[2].toInt())
                }

                filterCards()

                if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                    page--
                }

                applyResult(event)
            }
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val index = event.values[0].toInt()

                val card = cards[index]

                selectedCard.add(card)

                filterCards()

                if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                    page--
                }

                applyResult(event)
            }
            "all" -> {
                selectedCard.clear()

                inventory.cards.keys.filter { c -> c.tier == CardData.Tier.COMMON }
                        .forEach { c ->
                            repeat(inventory.cards[c] ?: 0) {
                                selectedCard.add(c)
                            }
                        }

                event.deferReply()
                        .setContent("Successfully added all of your cards! Keep in mind that you can't undo the task once you salvage the cards")
                        .setEphemeral(true)
                        .queue()

                filterCards()

                page = 0

                applyResult()
            }
            "reset" -> {
                selectedCard.clear()

                event.deferReply()
                        .setContent("Successfully reset your selections!")
                        .setEphemeral(true)
                        .queue()

                filterCards()

                page = 0

                applyResult()
            }
        }
    }

    private fun filterCards() {
        cards.clear()

        if (banner[0] == -1) {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.COMMON }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        } else {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.COMMON && c.unitID in CardData.bannerData[CardData.Tier.COMMON.ordinal][banner[1]] }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
                .setContent(getText())
                .setComponents(assignComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
                .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
                .setComponents(assignComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
                .queue()
    }

    private fun assignComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        CardData.bannerCategoryText[CardData.Tier.COMMON.ordinal].forEachIndexed { i, a ->
            bannerCategoryElements.add(SelectOption.of(a, "category-${CardData.Tier.COMMON.ordinal}-$i"))
        }

        val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

        val option = bannerCategoryElements.find { e -> e.value == "category-${CardData.Tier.COMMON.ordinal}-${banner[1]}" }

        if (option != null)
            bannerCategory.setDefaultOptions(option)

        rows.add(ActionRow.of(bannerCategory.build()))

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in page * SearchHolder.PAGE_CHUNK until min(dataSize, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
                .addOptions(cardCategoryElements)
                .setPlaceholder(
                        if (cards.isEmpty())
                            "No Cards To Select"
                        else if (!salvageMode && selectedCard.size == 10)
                            "Selected all 10 cards"
                        else
                            "Select Card"
                )
                .setDisabled(cards.isEmpty() || (!salvageMode && selectedCard.size == 10))
                .build()

        rows.add(ActionRow.of(cardCategory))

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS))
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS))
            }

            if(page + 1 >= totPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))
            }

            if(totPage > 10) {
                if(page + 10 >= totPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        if (salvageMode)
            confirmButtons.add(Button.primary("salvage", "Salvage").withDisabled(selectedCard.size < 10).withEmoji(Emoji.fromUnicode("\uD83E\uDE84")))
        else
            confirmButtons.add(Button.success("craft", "Craft T2 Card").withDisabled(selectedCard.size != 10).withEmoji(Emoji.fromUnicode("\uD83D\uDEE0\uFE0F")))

        confirmButtons.add(Button.secondary("all", "Add All").withDisabled(selectedCard.size == inventory.cards.keys.filter { c -> c.tier == CardData.Tier.COMMON }.sumOf { c -> inventory.cards[c] ?: 0 }))
        confirmButtons.add(Button.danger("reset", "Reset").withDisabled(selectedCard.isEmpty()))
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder(
            if (salvageMode)
                "Select 10 or more Tier 1 [Common] cards\n\n### Selected Cards\n\n"
            else
                "Select 10 Tier 1 [Common] cards to craft\n\n### Selected Cards\n\n"
        )

        if (selectedCard.isNotEmpty()) {
            val checker = StringBuilder()

            for (c in selectedCard) {
                checker.append("- ")
                        .append(c.cardInfo())
                        .append("\n")
            }

            if (checker.length > 1000) {
                builder.append("Selected ${selectedCard.size} cards\n")
            } else {
                builder.append(checker)
            }
        } else {
            builder.append("- No Cards Selected\n")
        }

        builder.append("\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = (inventory.cards[cards[i]] ?: 0) - selectedCard.filter { c -> cards[i].unitID == c.unitID }.size

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