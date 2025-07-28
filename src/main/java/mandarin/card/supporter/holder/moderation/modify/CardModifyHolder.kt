package mandarin.card.supporter.holder.moderation.modify

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.PositiveMap
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.modal.CardAmountSelectHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.min

class CardModifyHolder(author: Message, userID: String, channelID: String, message: Message, private val isAdd: Boolean, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val cards = ArrayList<Card>(
        if (isAdd) {
            CardData.cards.sortedWith(CardComparator())
        } else {
            inventory.cards.keys.union(inventory.favorites.keys).sortedWith(CardComparator())
        }
    )

    private val selectedCards = PositiveMap<Card, Int>()

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Inventory modification expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "category" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                banner = if (value == "all") {
                    Banner.NONE
                } else {
                    CardData.banners[value.toInt()]
                }

                page = 0

                filterCards()

                applyResult(event)
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                tier = if (value == "all") {
                    CardData.Tier.NONE
                } else {
                    CardData.Tier.entries[value.replace("tier", "").toInt()]
                }

                page = 0

                filterCards()

                applyResult(event)
            }
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val index = event.values[0].toInt()
                val card = cards[index]

                if (isAdd) {
                    val input = TextInput.create("amount", "Amount of Cards", TextInputStyle.SHORT)
                        .setPlaceholder("Put amount of cards that will be added")
                        .build()

                    val modal = Modal.create("select", "Select Amount of Cards")
                        .addComponents(ActionRow.of(input))
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardAmountSelectHolder(authorMessage, userID, channelID, message) { amount ->
                        selectedCards[card] = amount

                        filterCards()

                        if (cards.size <= page * ConfigHolder.SearchLayout.COMPACTED.chunkSize && page > 0) {
                            page--
                        }

                        applyResult()
                    })
                } else {
                    val realAmount = (inventory.cards[card] ?: 0) + (inventory.favorites[card] ?: 0) - (selectedCards[card] ?: 0)

                    if (realAmount >= 2) {
                        val input = TextInput.create("amount", "Amount of Cards", TextInputStyle.SHORT)
                            .setPlaceholder("Put amount up to $realAmount")
                            .build()

                        val modal = Modal.create("select", "Select Amount of Cards")
                            .addComponents(ActionRow.of(input))
                            .build()

                        event.replyModal(modal).queue()

                        connectTo(CardAmountSelectHolder(authorMessage, userID, channelID, message) { amount ->
                            val filteredAmount = min(amount, realAmount)

                            selectedCards[card] = filteredAmount

                            filterCards()

                            if (cards.size <= page * ConfigHolder.SearchLayout.COMPACTED.chunkSize && page > 0) {
                                page--
                            }

                            applyResult()
                        })
                    } else {
                        selectedCards[cards[index]] = 1

                        filterCards()

                        if (cards.size <= page * ConfigHolder.SearchLayout.COMPACTED.chunkSize && page > 0) {
                            page--
                        }

                        applyResult(event)
                    }
                }
            }
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
                page += 10

                applyResult(event)
            }
            "confirm" -> {
                if (isAdd) {
                    inventory.addCards(selectedCards)
                } else {
                    inventory.removeCards(selectedCards)
                }

                val m = event.member ?: return

                TransactionLogger.logCardsModify(selectedCards, m, targetMember, isAdd, !isAdd && inventory.cards.isNotEmpty() && inventory.favorites.isNotEmpty() && selectedCards.size == inventory.cards.keys.sumOf { c -> inventory.cards[c] ?: 0 })

                selectedCards.clear()

                event.deferReply()
                    .setContent("Successfully ${if (isAdd) "added" else "removed"} selected cards!")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "remove" -> {
                selectedCards.clear()

                inventory.cards.keys.forEach { c ->
                    selectedCards[c] = (inventory.cards[c] ?: 0)
                }

                inventory.favorites.keys.forEach { c ->
                    selectedCards[c] = (selectedCards[c] ?: 0) + (inventory.favorites[c] ?: 0)
                }

                event.deferReply()
                    .setContent("Selected all cards this user has. This will remove all cards from user. Please confirm this action")
                    .setEphemeral(true)
                    .queue()

                filterCards()

                applyResult()
            }
            "clear" -> {
                selectedCards.clear()

                event.deferReply()
                    .setContent("Successfully cleared selected cards!")
                    .setEphemeral(true)
                    .queue()

                filterCards()

                applyResult()
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                end(true)
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        filterCards()

        applyResult(event)
    }

    private fun filterCards() {
        cards.clear()

        cards.addAll(
            if (isAdd) {
                CardData.cards
            } else {
                inventory.cards.keys.union(inventory.favorites.keys)
            }
        )

        val collectedCards = banner.collectCards()

        if (banner !== Banner.NONE) {
            cards.removeIf { c -> c !in collectedCards }
        }

        if (tier != CardData.Tier.NONE) {
            cards.removeIf { c -> c.tier != tier }
        }

        if (!isAdd) {
            cards.removeIf { c -> (inventory.cards[c] ?: 0) + (inventory.favorites[c] ?: 0) - (selectedCards[c] ?: 0) <= 0 }
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun assignComponents() : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            val emoji = EmojiStore.getCardEmoji(
                when (index) {
                    0 -> null
                    1 -> CardPack.CardType.T1
                    2 -> CardPack.CardType.T2
                    3 -> CardPack.CardType.T3
                    4 -> CardPack.CardType.T4
                    else -> throw IllegalStateException("E/CardModifyHolder::assignComponents - Invalid tier index $index")
                }
            )

            tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        if (tier == CardData.Tier.NONE) {
            tierCategory.setDefaultOptions(tierCategoryElements[0])
        } else {
            tierCategory.setDefaultOptions(tierCategoryElements[tier.ordinal + 1])
        }

        rows.add(ActionRow.of(tierCategory.build()))

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(banner === Banner.NONE))
        val bannerList = if (tier != CardData.Tier.NONE) {
            CardData.banners.filter { b -> b.category && CardData.cards.any { c -> b in c.banner && c.tier == tier } }
        } else {
            CardData.banners.filter { b -> b.category }
        }

        bannerCategoryElements.addAll(bannerList.map { SelectOption.of(it.name, CardData.banners.indexOf(it).toString()).withDefault(it === banner) })

        if (bannerCategoryElements.size > 1) {
            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            rows.add(ActionRow.of(bannerCategory.build()))
        }

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(cards.size, (page + 1 ) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
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

        val totalPage = getTotalPage(cards.size)

        if (cards.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            if(totalPage > 10) {
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

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm").withDisabled(selectedCards.isEmpty()))
        confirmButtons.add(Button.danger("clear", "Clear").withDisabled(selectedCards.isEmpty()))

        if (!isAdd) {
            confirmButtons.add(Button.danger("remove", "Mass Remove").withDisabled(inventory.cards.isEmpty() || selectedCards.size == inventory.cards.keys.sumOf { c -> inventory.cards[c] ?: 0 }))
        }

        confirmButtons.add(Button.secondary("back", "Back"))
        confirmButtons.add(Button.danger("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder(
            if (isAdd)
                "Please select cards that will be added"
            else
                "Please select cards that will be removed"
        )

        builder.append("\n\n### Selected Cards\n\n")

        if (selectedCards.isEmpty()) {
            builder.append("- None\n")
        } else {
            val checker = StringBuilder()

            selectedCards.forEach { (card, amount) ->
                checker.append("- ")
                    .append(card.cardInfo())

                if (amount >= 2) {
                    checker.append(" x$amount")
                }

                checker.append("\n")
            }

            if (checker.length > 1500) {
                builder.append("- ${selectedCards.entries.sumOf { it.value }} Cards Selected")
            } else {
                builder.append(checker)
            }
        }

        builder.append("\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = if (isAdd)
                    1
                else
                    (inventory.cards[cards[i]] ?: 0) + (inventory.favorites[cards[i]] ?: 0) - (selectedCards[cards[i]] ?: 0)

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