package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.*
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.filter.Filter
import mandarin.card.supporter.log.LogSession
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

class FilterProcessHolder : ComponentHolder {
    private val product: Product
    private val filters: List<Filter>
    private val inventory: Inventory
    private val role: CardData.Role
    private val reward: Consumer<GenericComponentInteractionCreateEvent>

    private val cards = ArrayList<Card>()

    private val cardGroups: Array<ArrayList<Card>>

    private var currentIndex = 0

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    constructor(author: Message, userID: String, channelID: String, message: Message, product: Product, filters: List<Filter>, inventory: Inventory, role: CardData.Role) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.product = product
        this.filters = filters
        this.inventory = inventory
        this.role = role

        cardGroups = Array(filters.size) {
            ArrayList()
        }

        reward = Consumer {  }

        filterCards()
    }

    constructor(author: Message, userID: String, channelID: String, message: Message, product: Product, filters: List<Filter>, inventory: Inventory, reward: Consumer<GenericComponentInteractionCreateEvent>) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.product = product
        this.filters = filters
        this.inventory = inventory
        this.reward = reward

        cardGroups = Array(filters.size) {
            ArrayList()
        }

        role = CardData.Role.NONE

        filterCards()
    }

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Purchase expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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
                page += 10

                applyResult(event)
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
            "confirm" -> {
                if (currentIndex < product.requiredFilter - 1) {
                    currentIndex++

                    event.deferReply()
                        .setContent(
                            "Successfully applied selected cards for previous requirement, " +
                            if (cardGroups[currentIndex].size == filters[currentIndex].amount)
                                "it seems your selected cards satisfied current requirement too. You can skip to next requirement"
                            else
                                "select cards for next requirement"
                        )
                        .setEphemeral(true)
                        .queue()

                    filterCards()

                    applyResult()
                } else {
                    val totalCard = ArrayList<Card>()

                    cardGroups.forEach { g -> totalCard.addAll(g.toSet()) }

                    val spentCard = ArrayList<Card>()

                    for (card in totalCard.toSet()) {
                        val number = cardGroups.maxOf { g -> g.filter { c -> c.id == card.id }.size }

                        repeat(number) {
                            spentCard.add(card)
                        }

                        if ((inventory.cards[card] ?: 0) - number < 0) {
                            StaticStore.logger.uploadLog("W/FilterProcessHolder::onEvent - Bot found card with negative amount : ${card.cardInfo()}")
                        }

                        inventory.cards[card] = (inventory.cards[card] ?: 0) - number
                    }

                    if (role != CardData.Role.NONE) {
                        inventory.vanityRoles.add(role)

                        event.deferEdit()
                            .setContent("Successfully purchased role <@&${role.id}>!\n\nYou can always equip/unequip role via `${CardBot.globalPrefix}equip` command")
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .setComponents()
                            .queue()

                        TransactionLogger.logRolePurchase(authorMessage.author.idLong, role)
                    } else {
                        reward.accept(event)
                    }

                    LogSession.session.logBuy(authorMessage.author.idLong, spentCard)

                    end(true)
                }
            }
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

                val selectedCard = cards[index]

                val appliedFutureFilter = ArrayList<Filter>()

                //Handle future requirements
                if (currentIndex < product.requiredFilter - 1) {
                    for (i in currentIndex + 1 until product.requiredFilter) {
                        if (filters[i].filter(selectedCard) && cardGroups[i].size < filters[i].amount) {
                            appliedFutureFilter.add(filters[i])
                            cardGroups[i].add(selectedCard)
                        }
                    }
                }

                cardGroups[currentIndex].add(selectedCard)

                val builder = StringBuilder("Added `${selectedCard.simpleCardInfo()}` successfully")

                if (appliedFutureFilter.isNotEmpty()) {
                    builder.append("\n\nThis card met other requirements too, so bot automatically applied same card to those requirements\n\n### Applied requirements\n\n")

                    for (requirement in appliedFutureFilter) {
                        builder.append("- ")
                            .append(requirement.name)
                            .append("\n")
                    }
                }

                event.deferReply()
                    .setContent(builder.toString())
                    .setEphemeral(true)
                    .queue()

                filterCards()

                if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                    page--
                }

                applyResult()
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Purchase canceled")
                    .setAllowedMentions(ArrayList())
                    .setComponents()
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
            event.deferEdit()
                .setContent("It seems you can't afford this role with your cards")
                .setAllowedMentions(ArrayList())
                .setComponents(getComponents())
                .mentionRepliedUser(false)
                .queue()

            return
        } else {
            val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

            if (doableFilters.size < product.requiredFilter) {
                event.deferEdit()
                    .setContent("It seems you can't afford this role with your cards")
                    .setAllowedMentions(ArrayList())
                    .setComponents(getComponents())
                    .mentionRepliedUser(false)
                    .queue()

                return
            }
        }

        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun filterCards() {
        cards.clear()

        cards.addAll(inventory.cards.keys)

        val collectedCards = banner.collectCards()

        if (banner !== Banner.NONE) {
            cards.removeIf { c -> c !in collectedCards }
        }

        if (tier != CardData.Tier.NONE) {
            cards.removeIf { c -> c.tier != tier }
        }

        cards.removeIf { c ->
            if (c.id < 0)
                return@removeIf true

            val amount = inventory.cards[c] ?: 1

            amount - cardGroups.maxOf { g -> g.filter { card -> card.id == c.id }.size } <= 0
        }

        cards.removeIf { c -> !filters[currentIndex].filter(c) }

        cards.sortWith(CardComparator())

        page = max(0, min(page, getTotalPage(cards.size) - 1))
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val impossibleFilter = filters.find { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } < f.amount }

        if (impossibleFilter != null) {
            result.add(ActionRow.of(Button.secondary("back", "Back")))

            return result
        }

        if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
            result.add(ActionRow.of(Button.secondary("back", "Back")))

            return result
        } else {
            val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

            if (doableFilters.size < product.requiredFilter) {
                result.add(ActionRow.of(Button.secondary("back", "Back")))

                return result
            }
        }

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            if (CardData.Tier.SPECIAL.ordinal != index) {
                val emoji = EmojiStore.getCardEmoji(
                    when (index) {
                        1 -> CardPack.CardType.T1
                        2 -> CardPack.CardType.T2
                        3 -> CardPack.CardType.T3
                        4 -> CardPack.CardType.T4
                        else -> throw IllegalStateException("E/FilterProcessHolder::getComponents - Invalid tier index $index")
                    }
                )

                tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
            }
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        if (tier == CardData.Tier.NONE) {
            tierCategory.setDefaultOptions(tierCategoryElements[0])
        } else {
            val option = tierCategoryElements.find { option -> option.value == "tier${tier.ordinal}" }

            tierCategory.setDefaultOptions(option)
        }

        result.add(ActionRow.of(tierCategory.build()))

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

            result.add(ActionRow.of(bannerCategory.build()))
        }

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in SearchHolder.PAGE_CHUNK * page until min(dataSize, SearchHolder.PAGE_CHUNK * (page + 1))) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (cards.isEmpty())
                    "No Cards To Select"
                else if (cardGroups[currentIndex].size == filters[currentIndex].amount)
                    "Condition met"
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty() || cardGroups[currentIndex].size == filters[currentIndex].amount)
            .build()

        result.add(ActionRow.of(cardCategory))

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

            result.add(ActionRow.of(buttons))
        }

        result.add(
            ActionRow.of(
                Button.success("confirm", "Confirm").withDisabled(filters[currentIndex].amount != cardGroups[currentIndex].size),
                Button.secondary("back", "Back"),
                Button.danger("cancel", "Cancel")
            )
        )

        return result
    }

    private fun getText() : String {
        val impossibleFilter = filters.find { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } < f.amount }

        if (impossibleFilter != null) {
            return "It seems you selected requirement that you can't afford with your inventory, please select other requirements\n\nRequirement causing problem : ${impossibleFilter.name}"
        }

        val builder = StringBuilder("Please select cards that meets the requirement\n\nCondition : ${filters[currentIndex].name}\n\n### Cards\n\n")

        if (cardGroups[currentIndex].isEmpty()) {
            builder.append("- No Cards Selected\n")
        } else {
            for (card in cardGroups[currentIndex]) {
                builder.append("- ")
                    .append(card.cardInfo())
                    .append("\n")
            }
        }

        builder.append("\n```md\n")

        if (cards.isEmpty()) {
            builder.append("No Cards")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(cards.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1)
                    .append(". ")
                    .append(cards[i].cardInfo())

                val amount = (inventory.cards[cards[i]] ?: 1) - cardGroups.maxOf { g -> g.filter { card -> card.id == cards[i].id }.size }

                if (amount > 1) {
                    builder.append(" x$amount")
                }

                builder.append("\n")
            }
        }

        builder.append("```")

        return builder.toString()
    }
}