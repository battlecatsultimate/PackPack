package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.pack.CardPack
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
import kotlin.math.min

class CardRankingSelectHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val cards = CardData.cards.sortedWith(CardComparator()).toMutableList()

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    init {
        registerAutoExpiration(FIVE_MIN)
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

                connectTo(event, CardRankingListHolder(authorMessage, userID, channelID, message, card))
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
            "close" -> {
                event.deferEdit()
                    .setContent("Card ranking closed")
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
        message.editMessage("Card ranking expired").setComponents().queue()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun filterCards() {
        cards.clear()

        cards.addAll(CardData.cards)

        val collectedCards = banner.collectCards()

        if (banner !== Banner.NONE) {
            cards.removeIf { c -> c !in collectedCards }
        }

        if (tier != CardData.Tier.NONE) {
            cards.removeIf { c -> c.tier != tier }
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

    private fun assignComponents() : List<LayoutComponent> {
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

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in page * SearchHolder.PAGE_CHUNK until min(dataSize, (page + 1 ) * SearchHolder.PAGE_CHUNK)) {
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

        val totalPage = getTotalPage(dataSize)

        if (dataSize > SearchHolder.PAGE_CHUNK) {
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

        confirmButtons.add(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder("Please select the card that you want to see the ranking of\n\n```md\n")

        if (cards.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}\n")
            }
        } else {
            builder.append("No Cards Found")
        }

        builder.append("```")

        return builder.toString()
    }
}