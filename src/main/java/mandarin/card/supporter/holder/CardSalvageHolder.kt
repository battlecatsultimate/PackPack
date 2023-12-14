package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
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

class CardSalvageHolder(author: Message, channelID: String, private val message: Message, private val salvageMode: CardData.SalvageMode) : ComponentHolder(author, channelID, message.id) {
    private val inventory = Inventory.getInventory(author.author.id)
    private val tier = if (salvageMode == CardData.SalvageMode.T3) CardData.Tier.ULTRA else CardData.Tier.COMMON
    private val cards = ArrayList<Card>(inventory.cards.keys.filter { c -> c.tier == tier }.sortedWith(CardComparator()))

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
                val cf = selectedCard.size * (if (salvageMode == CardData.SalvageMode.T3) CardData.Tier.ULTRA.cost else CardData.Tier.COMMON.cost)

                inventory.removeCards(selectedCard)

                inventory.catFoods += cf

                TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size, selectedCard)

                event.deferEdit()
                    .setContent("Salvaged ${selectedCard.size} cards, and you received ${EmojiStore.ABILITY["CF"]?.formatted} $cf!")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                CardBot.saveCardData()

                expired = true
                expire(authorMessage.author.id)
            }
            "craft" -> {
                inventory.removeCards(selectedCard)

                val chance = Random.nextDouble()

                if (chance <= 0.7) {
                    val cf = selectedCard.size * CardData.Tier.COMMON.cost

                    inventory.catFoods += cf

                    TransactionLogger.logCraft(authorMessage.author.idLong, selectedCard.size, null, selectedCard)

                    event.deferEdit()
                        .setContent("Failed to craft tier 2 card..., so you received ${EmojiStore.ABILITY["CF"]?.formatted} $cf")
                        .setComponents()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(ArrayList())
                        .queue()
                } else {
                    val card = CardData.appendUncommon(CardData.uncommon).random()

                    TransactionLogger.logCraft(authorMessage.author.idLong, selectedCard.size, card, selectedCard)

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

                inventory.cards.keys.filter { c -> c.tier == tier }
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
            "dupe" -> {
                val duplicatedCards = inventory.cards.keys
                    .filter { c -> c.tier == tier }
                    .filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID }.size > 1 }
                    .sortedWith { c, c2 ->
                    val thatOne = (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID }.size
                    val thisOne = (inventory.cards[c2] ?: 0) - selectedCard.filter { card -> card.unitID == c2.unitID }.size

                    thisOne.compareTo(thatOne)
                }

                for (c in duplicatedCards) {
                    if (selectedCard.size == 10)
                        break

                    repeat(min(10 - selectedCard.size, (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID }.size - 1)) {
                        selectedCard.add(c)
                    }
                }

                event.deferReply()
                    .setContent("Successfully added all of your duplicated cards as much as possible!")
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
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        } else {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier && c.unitID in CardData.bannerData[tier.ordinal][banner[1]] }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
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

        CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
            bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
        }

        val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

        val option = bannerCategoryElements.find { e -> e.value == "category-${tier.ordinal}-${banner[1]}" }

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
                        else if (salvageMode == CardData.SalvageMode.T1 && selectedCard.size == 10)
                            "Selected all 10 cards"
                        else if (salvageMode == CardData.SalvageMode.T3 && selectedCard.size == 1)
                            "Selected 1 card"
                        else
                            "Select Card"
                )
                .setDisabled(cards.isEmpty() || (salvageMode == CardData.SalvageMode.T1 && selectedCard.size == 10) || (salvageMode == CardData.SalvageMode.T3 && selectedCard.size == 1))
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

        if (salvageMode == CardData.SalvageMode.T1 || salvageMode == CardData.SalvageMode.T3)
            confirmButtons.add(Button.primary("salvage", "Salvage").withDisabled(selectedCard.size < if(salvageMode == CardData.SalvageMode.T1) 10 else 1).withEmoji(Emoji.fromUnicode("\uD83E\uDE84")))
        else
            confirmButtons.add(Button.success("craft", "Craft T2 Card").withDisabled(selectedCard.size != 10).withEmoji(Emoji.fromUnicode("\uD83D\uDEE0\uFE0F")))

        if (salvageMode == CardData.SalvageMode.T1)
            confirmButtons.add(Button.secondary("all", "Add All").withDisabled(selectedCard.size == inventory.cards.keys.filter { c -> c.tier == tier }.sumOf { c -> inventory.cards[c] ?: 0 }))
        else if (salvageMode == CardData.SalvageMode.CRAFT)
            confirmButtons.add(Button.secondary("dupe", "Use Duplicated").withDisabled(!inventory.cards.keys.filter { c -> c.tier == tier }.any { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID }.size > 1 } || selectedCard.size == 10))

        confirmButtons.add(Button.danger("reset", "Reset").withDisabled(selectedCard.isEmpty()))
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder(
            when (salvageMode) {
                CardData.SalvageMode.T1 -> "Select 10 or more Tier 1 [Common] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.CRAFT -> "Select 10 Tier 1 [Common] cards to craft\n\n### Selected Cards\n\n"
                else -> "Select 1 Tier 3 [Ultra Rare (Exclusives)] card\n\n### Selected card\n\n"
            }
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