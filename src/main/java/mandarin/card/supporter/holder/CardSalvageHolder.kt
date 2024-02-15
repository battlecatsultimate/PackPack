package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.modal.CardAmountSelectHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
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
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.min

class CardSalvageHolder(author: Message, channelID: String, private val message: Message, private val salvageMode: CardData.SalvageMode) : ComponentHolder(author, channelID, message.id) {
    private val inventory = Inventory.getInventory(author.author.id)
    private val tier = when(salvageMode) {
        CardData.SalvageMode.T1 -> CardData.Tier.COMMON
        CardData.SalvageMode.T2,
        CardData.SalvageMode.SEASONAL,
        CardData.SalvageMode.COLLAB -> CardData.Tier.UNCOMMON
        CardData.SalvageMode.T3 -> CardData.Tier.ULTRA
    }
    private val cards = ArrayList<Card>(inventory.cards.keys.filter { c -> c.tier == tier && c.unitID != 435 && c.unitID != 484 }.sortedWith(CardComparator()))

    private val selectedCard = ArrayList<Card>()

    private var page = 0
    private var banner = intArrayOf(-1, -1)

    init {
        filterCards()
    }

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
                val shard = selectedCard.size * salvageMode.cost

                val salvage: (GenericComponentInteractionCreateEvent) -> Unit = {
                    inventory.removeCards(selectedCard)

                    inventory.platinumShard += shard

                    TransactionLogger.logSalvage(authorMessage.author.idLong, selectedCard.size, salvageMode, selectedCard)

                    it.deferEdit()
                        .setContent("Salvaged ${selectedCard.size} cards, and you received ${EmojiStore.ABILITY["SHARD"]?.formatted} $shard!")
                        .setComponents()
                        .mentionRepliedUser(false)
                        .setAllowedMentions(ArrayList())
                        .queue()

                    CardBot.saveCardData()

                    expired = true

                    expire(authorMessage.author.id)
                }

                if (shard >= 100 || selectedCard.size >= 20) {
                    event.deferEdit()
                        .setContent("Are you sure you are fine with salvaging selected cards?\n" +
                                "\n" +
                                "**Salvaging means you will lose selected cards from your inventory and gain platinum shards as return. You can't undo the process once you confirm it**")
                        .setComponents(toConfirmationPopUp())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, {
                        salvage.invoke(it)

                        return@ConfirmPopUpHolder null
                    }, {
                        applyResult(it)

                        StaticStore.putHolder(authorMessage.author.id, this)

                        return@ConfirmPopUpHolder null
                    }, LangID.EN))
                } else {
                    salvage.invoke(event)
                }
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

                val realAmount = (inventory.cards[card] ?: 0) - selectedCard.count { c -> c.unitID == card.unitID }

                if (realAmount > 2) {
                    val input = TextInput.create("amount", "Amount of Cards", TextInputStyle.SHORT)
                        .setPlaceholder("Put amount up to ${realAmount - 1L}")
                        .build()

                    val modal = Modal.create("select", "Select Amount of Cards")
                        .addActionRow(input)
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardAmountSelectHolder(authorMessage, channelID, message.id) { amount ->
                        val filteredAmount = min(amount, realAmount - 1)

                        repeat(filteredAmount) {
                            selectedCard.add(card)
                        }

                        filterCards()

                        if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                            page--
                        }

                        applyResult()
                    })
                } else {
                    selectedCard.add(card)

                    filterCards()

                    if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                        page--
                    }

                    applyResult(event)
                }
            }
            "all" -> {
                val adder: (GenericComponentInteractionCreateEvent) -> Unit = {
                    selectedCard.clear()

                    cards.forEach { c ->
                        repeat(inventory.cards[c] ?: 0) {
                            selectedCard.add(c)
                        }
                    }

                    it.deferReply()
                        .setContent("Successfully added all of your cards! Keep in mind that you can't undo the task once you salvage the cards")
                        .setEphemeral(true)
                        .queue()

                    filterCards()

                    page = 0

                    applyResult()

                    StaticStore.putHolder(authorMessage.author.id, this)
                }

                event.deferEdit()
                    .setContent("Are you sure you want to add all cards?")
                    .setComponents(toConfirmationPopUp())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, {
                    adder.invoke(it)

                    return@ConfirmPopUpHolder null
                }, {
                    applyResult(it)

                    StaticStore.putHolder(authorMessage.author.id, this)

                    return@ConfirmPopUpHolder null
                }, LangID.EN))
            }
            "dupe" -> {
                cards.forEach { c ->
                        val amount = (inventory.cards[c] ?: 0)

                        if (amount >= 2) {
                            repeat(amount - 1) {
                                selectedCard.add(c)
                            }
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
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        filterCards()

        applyResult(event)
    }

    private fun filterCards() {
        cards.clear()

        if (banner[0] == -1) {
            cards.addAll(
                inventory.cards.keys.filter { c -> c.tier == tier && c.unitID != 435 && c.unitID != 484 }
                    .filter { c ->
                        when (salvageMode) {
                            CardData.SalvageMode.T2 -> c.isRegularUncommon()
                            CardData.SalvageMode.SEASONAL -> c.isSeasonalUncommon()
                            CardData.SalvageMode.COLLAB -> c.isCollaborationUncommon()
                            else -> true
                        }
                    }
                    .filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 }
            )
        } else {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier && c.unitID in CardData.bannerData[tier.ordinal][banner[1]] }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
                .setContent(getText())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
                .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
                .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val pageButtons = ArrayList<Button>()

        pageButtons.add(Button.secondary("back", "Back"))
        pageButtons.add(Button.danger("cancel", "Cancel"))

        if (cards.isEmpty() && selectedCard.isEmpty()) {
            rows.add(ActionRow.of(pageButtons))

            return rows
        }

        if (salvageMode != CardData.SalvageMode.SEASONAL && salvageMode != CardData.SalvageMode.COLLAB) {
            val bannerCategoryElements = ArrayList<SelectOption>()

            bannerCategoryElements.add(SelectOption.of("All", "all"))

            if (tier != CardData.Tier.UNCOMMON) {
                CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                    bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
                }
            } else {
                CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                    if (i < 2) {
                        bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
                    }
                }
            }

            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            val option = bannerCategoryElements.find { e -> e.value == "category-${tier.ordinal}-${banner[1]}" }

            if (option != null)
                bannerCategory.setDefaultOptions(option)

            rows.add(ActionRow.of(bannerCategory.build()))
        }

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

        confirmButtons.add(Button.primary("salvage", "Salvage").withDisabled(selectedCard.isEmpty()).withEmoji(Emoji.fromUnicode("\uD83E\uDE84")))

        val duplicated = inventory.cards.keys.filter { c -> c.tier == tier && c.unitID != 435 && c.unitID != 484 }
            .filter { c ->
                when (salvageMode) {
                    CardData.SalvageMode.T2 -> c.unitID in BannerFilter.Banner.TheAlimighties.getBannerData() || c.unitID in BannerFilter.Banner.GirlsAndMonsters.getBannerData()
                    CardData.SalvageMode.SEASONAL -> c.unitID in BannerFilter.Banner.Seasonal.getBannerData()
                    CardData.SalvageMode.COLLAB -> c.unitID in BannerFilter.Banner.Collaboration.getBannerData()
                    else -> true
                }
            }
            .filter { c ->
                (inventory.cards[c] ?: 0 ) - selectedCard.count { card -> card.unitID == c.unitID } > 1
            }

        confirmButtons.add(Button.secondary("dupe", "Add Duplicated").withDisabled(duplicated.isEmpty()))
        confirmButtons.add(Button.secondary("all", "Add All").withDisabled(selectedCard.size == inventory.cards.keys.filter { c -> c.tier == tier }.sumOf { c -> inventory.cards[c] ?: 0 }))

        confirmButtons.add(Button.danger("reset", "Reset").withDisabled(selectedCard.isEmpty()))

        rows.add(ActionRow.of(confirmButtons))
        rows.add(ActionRow.of(pageButtons))

        return rows
    }

    private fun getText() : String {
        if (cards.isEmpty() && selectedCard.isEmpty()) {
            return "You don't have any cards that meet condition of selected type!"
        }

        val builder = StringBuilder(
            when (salvageMode) {
                CardData.SalvageMode.T1 -> "Select Tier 1 [Common] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.T2 -> "Select Regular Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.SEASONAL -> "Select Seasonal Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                CardData.SalvageMode.COLLAB -> "Select Collaboration Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n"
                else -> "Select Tier 3 [Ultra Rare (Exclusives)] cards\n\n### Selected card\n\n"
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

        if (selectedCard.isNotEmpty()) {
            builder.append("\n${EmojiStore.ABILITY["SHARD"]?.formatted} ${selectedCard.size * salvageMode.cost} in total")
        } else {
            builder.append("\n${EmojiStore.ABILITY["SHARD"]?.formatted} 0 in total")
        }

        return builder.toString()
    }

    private fun toConfirmationPopUp() : List<LayoutComponent> {
        return arrayListOf(ActionRow.of(Button.success("confirm", "Confirm"), Button.danger("cancel", "Cancel")))
    }
}