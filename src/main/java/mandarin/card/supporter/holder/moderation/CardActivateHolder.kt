package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
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
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class CardActivateHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val cards = ArrayList<Card>(CardData.cards.sortedWith(CardComparator()))

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = intArrayOf(-1, -1)

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1))
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
                    intArrayOf(-1, -1)
                } else {
                    val data = value.split("-")

                    intArrayOf(data[1].toInt(), data[2].toInt())
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

                banner[0] = -1
                banner[1] = -1

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

                if (card in CardData.deactivatedCards) {
                    CardData.deactivatedCards.remove(card)
                } else {
                    CardData.deactivatedCards.add(card)
                }

                CardBot.saveCardData()

                applyResult(event)

                val m = event.member ?: return

                TransactionLogger.logCardActivate(m, card)
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
                event.deferReply()
                    .setContent("Successfully configured cards activation!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Card Activation closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Cards activation expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun filterCards() {
        cards.clear()

        val tempCards = CardData.cards

        if (tier != CardData.Tier.NONE) {
            if (banner[0] == -1) {
                cards.addAll(tempCards.filter { c -> c.tier == tier })
            } else {
                cards.addAll(tempCards.filter { c -> c.tier == tier && c.unitID in CardData.bannerData[tier.ordinal][banner[1]] })
            }
        } else {
            if (banner[0] == -1) {
                cards.addAll(tempCards)
            } else {
                cards.addAll(tempCards.filter { c -> c.unitID in CardData.bannerData[banner[0]][banner[1]] })
            }
        }

        cards.sortWith(CardComparator())

        page = max(0, min(page, getTotalPage(cards.size) - 1))
    }

    private fun getComponents() : List<LayoutComponent> {
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

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        if (tier == CardData.Tier.NONE) {
            CardData.bannerCategoryText.forEachIndexed { index, array ->
                array.forEachIndexed { i, a ->
                    bannerCategoryElements.add(SelectOption.of(a, "category-$index-$i"))
                }
            }
        } else {
            CardData.bannerCategoryText[tier.ordinal].forEachIndexed { i, a ->
                bannerCategoryElements.add(SelectOption.of(a, "category-${tier.ordinal}-$i"))
            }
        }

        val bannerCategory = StringSelectMenu.create("category")
            .addOptions(bannerCategoryElements)
            .setPlaceholder("Filter Cards by Banners")

        val id = if (tier == CardData.Tier.NONE) {
            "category-${banner[0]}-${banner[1]}"
        } else {
            "category-${tier.ordinal}-${banner[1]}"
        }

        val option = bannerCategoryElements.find { e -> e.value == id }

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
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty())
            .build()

        rows.add(ActionRow.of(cardCategory))

        var totalPage = getTotalPage(cards.size)

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

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm"))
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getContents() : String {
        val activate = EmojiStore.SWITCHON.formatted
        val deactivate = EmojiStore.SWITCHOFF.formatted

        val builder = StringBuilder(
            "Please select cards to toggle activation. Cards which have $activate emoji are " +
                    "activated, and users will be able to get them via rolling the pack or the slot " +
                    "machine. Cards which have $deactivate emoji are deactivated. Users can own these" +
                    "or trade them, but they can't obtain more from rolling the pack or the slot machine"
        )

        builder.append("\n\n### Cards List\n\n")

        if (cards.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min(cards.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                if (cards[i] in CardData.deactivatedCards)
                    builder.append(" ").append(deactivate)
                else
                    builder.append(" ").append(activate)

                builder.append("\n")
            }
        } else {
            builder.append("No Cards Found")
        }

        return builder.toString()
    }
}