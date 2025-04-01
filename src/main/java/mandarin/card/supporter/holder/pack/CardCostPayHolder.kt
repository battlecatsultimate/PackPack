package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.modal.CardAmountSelectHolder
import mandarin.card.supporter.pack.*
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class CardCostPayHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val container: CardPayContainer,
    private val containers: Array<CardPayContainer>
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)
    private val cards = ArrayList(inventory.cards.keys)

    private var page = 0
    private val tier = if (container.cost is SpecificCardCost || container.cost is BannerCardCost) {
        CardData.Tier.NONE
    } else {
        when(when(container.cost) {
            is TierCardCost -> container.cost.tier
            else -> throw IllegalStateException("E/CardCostPayHolder::init - Unknown cost type : ${container.cost::javaClass}")
        }) {
            CardPack.CardType.T1 -> CardData.Tier.COMMON
            CardPack.CardType.T2,
            CardPack.CardType.REGULAR,
            CardPack.CardType.SEASONAL,
            CardPack.CardType.COLLABORATION -> CardData.Tier.UNCOMMON
            CardPack.CardType.T3 -> CardData.Tier.ULTRA
            CardPack.CardType.T4 -> CardData.Tier.LEGEND
        }
    }

    private var banner = when(container.cost) {
        is TierCardCost -> Banner.NONE
        is BannerCardCost -> container.cost.banner
        is SpecificCardCost -> Banner.NONE
        else -> throw IllegalStateException("E/CardCostPayHolder::init - Unknown cost type : ${container.cost::javaClass}")
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
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val selectedID = event.values[0].toInt()

                if (selectedID < 0 || selectedID >= cards.size)
                    return

                val card = cards[selectedID]

                val realAmount = (inventory.cards[card] ?: 0) - containers.sumOf { container -> container.pickedCards.count { c -> c.id == card.id } }

                if (realAmount > 2 && container.cost.amount - container.pickedCards.size > 1) {
                    val input = TextInput.create("amount", "Amount of Cards", TextInputStyle.SHORT)
                        .setPlaceholder("Put amount up to ${min(realAmount - 1L, container.cost.amount - container.pickedCards.size)}")
                        .build()

                    val modal = Modal.create("select", "Select Amount of Cards")
                        .addActionRow(input)
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardAmountSelectHolder(authorMessage, userID, channelID, message) { amount ->
                        val filteredAmount = min(min(amount, realAmount - 1).toLong(), container.cost.amount - container.pickedCards.size)

                        repeat(filteredAmount.toInt()) {
                            container.pickedCards.add(card)
                        }

                        filterCards()

                        if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                            page--
                        }

                        applyResult()
                    })
                } else {
                    container.pickedCards.add(card)

                    filterCards()

                    if (cards.size <= page * SearchHolder.PAGE_CHUNK && page > 0) {
                        page--
                    }

                    applyResult(event)
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
            "dupe" -> {
                for (card in cards) {
                    val amount = (inventory.cards[card] ?: 0) - containers.sumOf { c -> c.pickedCards.count { cd -> cd.id == card.id } }

                    if (amount > 1) {
                        repeat(min(amount - 1, container.cost.amount.toInt() - container.pickedCards.size)) {
                            container.pickedCards.add(card)
                        }
                    }

                    if (container.paid())
                        break
                }

                event.deferReply()
                    .setContent("Successfully added all duplicated cards! Check the result above")
                    .setEphemeral(true)
                    .queue()

                filterCards()
                applyResult()
            }
            "all" -> {
                for (card in cards) {
                    val amount = (inventory.cards[card] ?: 0) - containers.sumOf { c -> c.pickedCards.count { cd -> cd.id == card.id } }

                    if (amount > 0) {
                        repeat(min(amount, container.cost.amount.toInt() - container.pickedCards.size)) {
                            container.pickedCards.add(card)
                        }
                    }

                    if (container.paid())
                        break
                }

                event.deferReply()
                    .setContent("Successfully added all cards! Check the result above")
                    .setEphemeral(true)
                    .queue()

                filterCards()
                applyResult()
            }
            "clear" -> {
                container.pickedCards.clear()

                event.deferReply()
                    .setContent("Successfully cleared selected cards for this cost!")
                    .setEphemeral(true)
                    .queue()

                filterCards()
                applyResult()
            }
            "confirm" -> {
                event.deferEdit().queue()

                goBack()
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
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        filterCards()
        applyResult(event)
    }

    private fun applyResult() {
        var builder = message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        if (event !is GenericComponentInteractionCreateEvent)
            return

        var builder = event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun filterCards() {
        cards.clear()

        if (container.cost is SpecificCardCost) {
            cards.addAll(inventory.cards.keys.filter { c -> container.cost.cards.any { card -> card.id == c.id} })
        } else {
            cards.addAll(inventory.cards.keys)

            if (tier != CardData.Tier.NONE) {
                cards.removeIf { c -> c.tier != tier }
            }

            if (banner != Banner.NONE) {
                cards.removeIf { c -> banner in c.banner }
            }

            cards.removeIf { c -> !container.cost.filter(c) }

            cards.removeIf { card ->
                val amount = inventory.cards[card] ?: 0
                val selectedAmount = containers.sumOf { container -> container.pickedCards.count { c -> c.id == card.id } }

                amount - selectedAmount <= 0
            }
        }

        cards.sortWith(CardComparator())

        page = max(0, min(page, getTotalPage(cards.size) - 1))
    }

    private fun getContent() : String {
        val builder = StringBuilder("Required cost : ")
            .append(container.cost.getCostName())
            .append("\n\n### Selected Cards\n")

        if (container.pickedCards.isEmpty()) {
            builder.append("- No cards selected\n")
        } else {
            val selectedCards = StringBuilder()

            container.pickedCards.toSet().forEach { card ->
                val amount = container.pickedCards.count { c -> c.id == card.id }

                selectedCards.append(card.simpleCardInfo())

                if (amount > 1)
                    selectedCards.append(" x").append(amount)

                selectedCards.append("\n")
            }

            if (selectedCards.length >= 1000) {
                builder.append("Selected ${container.pickedCards.size} card(s)\n")
            } else {
                builder.append(selectedCards)
            }
        }

        builder.append("\n### Card List\n```md\n")

        if (cards.isEmpty()) {
            builder.append("No cards")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                val amount = (inventory.cards[cards[i]] ?: 0) - containers.sumOf { c -> c.pickedCards.count { card -> card.id == cards[i].id } }

                builder.append(i + 1).append(". ").append(cards[i].simpleCardInfo())

                if (amount > 1)
                    builder.append(" x").append(amount)

                builder.append("\n")
            }
        }

        builder.append("```")

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (container.cost is TierCardCost) {
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
                if (container.paid())
                    "You selected enough cards"
                else if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card To Pay"
            )
            .setDisabled(container.paid() || cards.isEmpty())
            .build()

        result.add(ActionRow.of(cardCategory))

        val totalPage = ceil(dataSize * 1.0 / SearchHolder.PAGE_CHUNK)

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("confirm", "Confirm"))
        confirmButtons.add(Button.secondary("dupe", "Select Duplicated").withDisabled(container.paid() || !cards.any { card ->
            (inventory.cards[card] ?: 0) - containers.sumOf { ct -> ct.pickedCards.count { c -> c.id == card.id } } > 1
        }))
        confirmButtons.add(Button.secondary("all", "Add All").withDisabled(container.paid() || cards.isEmpty()))
        confirmButtons.add(Button.danger("clear", "Clear").withDisabled(container.pickedCards.isEmpty()))

        result.add(ActionRow.of(confirmButtons))

        return result
    }
}