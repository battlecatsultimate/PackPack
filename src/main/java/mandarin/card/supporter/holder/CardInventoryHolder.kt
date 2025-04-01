package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
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
import kotlin.math.max
import kotlin.math.min

class CardInventoryHolder(author: Message, userID: String, channelID: String, message: Message, private val inventory: Inventory, private val member: Member) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private enum class FilterMode {
        NONE,
        FAVORITE_ONLY,
        NON_FAVORITE_ONLY
    }

    private val cards = ArrayList<Card>(inventory.cards.keys.union(inventory.favorites.keys).sortedWith(CardComparator()))

    private var page = 0
        set(value) {
            field = value

            val totalPage = getTotalPage(cards.size)

            field = max(0, min(field, totalPage - 1))
        }
    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    private var filterMode = FilterMode.NONE

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Inventory expired")
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
            "filter" -> {
                filterMode = FilterMode.entries[(filterMode.ordinal + 1) % FilterMode.entries.size]

                filterCards()

                applyResult(event)
            }
            "confirm" -> {
                event.deferEdit()
                    .setContent("Inventory closed")
                    .setComponents(ArrayList())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
            "category" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                banner = when(value) {
                    "all" -> Banner.NONE
                    "seasonal" -> Banner.SEASONAL
                    "collab" -> Banner.COLLABORATION
                    else -> CardData.banners[value.toInt()]
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

                connectTo(event, CardFavoriteHolder(authorMessage, userID, channelID, message, inventory, card))
            }
        }
    }

    override fun onBack(child: Holder) {
        message.editMessage(getContents())
            .setEmbeds()
            .setFiles()
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun filterCards() {
        cards.clear()

        cards.addAll(inventory.cards.keys.union(inventory.favorites.keys))

        val collectedCards = banner.collectCards()

        if (banner !== Banner.NONE) {
            cards.removeIf { c -> c !in collectedCards }
        }

        if (tier != CardData.Tier.NONE) {
            cards.removeIf { c -> c.tier != tier }
        }

        if (filterMode == FilterMode.FAVORITE_ONLY) {
            cards.removeIf { card ->
                return@removeIf !inventory.favorites.containsKey(card)
            }
        } else if (filterMode == FilterMode.NON_FAVORITE_ONLY) {
            cards.removeIf { card ->
                return@removeIf !inventory.cards.containsKey(card)
            }
        }

        cards.sortWith(CardComparator())

        page = max(0, min(page, getTotalPage(cards.size) - 1))
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
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

        bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(banner === Banner.NONE))
        bannerCategoryElements.add(SelectOption.of("Seasonal Cards", "seasonal").withDefault(banner === Banner.SEASONAL))
        bannerCategoryElements.add(SelectOption.of("Collaboration Cards", "collab").withDefault(banner === Banner.COLLABORATION))

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
                    "Select Card To See"
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

        val text = when(filterMode) {
            FilterMode.NONE -> "Filter Mode : None"
            FilterMode.FAVORITE_ONLY -> "Filter Mode : Favorites Only"
            FilterMode.NON_FAVORITE_ONLY -> "Filter Mode : Non-Favorites Only"
        }

        confirmButtons.add(Button.primary("confirm", "Confirm").withEmoji(EmojiStore.CROSS))
        confirmButtons.add(Button.secondary("filter", text))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getContents() : String {
        val cardAmount = cards.sumOf { c ->
            return@sumOf when(filterMode) {
                FilterMode.NONE -> (inventory.cards[c] ?: 0) + (inventory.favorites[c] ?: 0)
                FilterMode.FAVORITE_ONLY -> inventory.favorites[c] ?: 0
                FilterMode.NON_FAVORITE_ONLY -> inventory.cards[c] ?: 0
            }
        }

        val start = if (cardAmount >= 2) {
            "Inventory of ${member.asMention}\n\nNumber of Filtered Cards : $cardAmount cards\n\n```md\n"
        } else if (cardAmount == 1) {
            "Inventory of ${member.asMention}\n\nNumber of Filtered Cards : $cardAmount card\n\n```md\n"
        } else {
            "Inventory of ${member.asMention}\n\n```md\n"
        }

        val builder = StringBuilder(start)

        if (cards.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ")

                if (filterMode != FilterMode.NON_FAVORITE_ONLY && inventory.favorites.containsKey(cards[i])) {
                    builder.append("⭐")
                }

                builder.append(cards[i].cardInfo())

                val amount = when(filterMode) {
                    FilterMode.NONE -> (inventory.cards[cards[i]] ?: 0) + (inventory.favorites[cards[i]] ?: 0)
                    FilterMode.FAVORITE_ONLY -> inventory.favorites[cards[i]] ?: 0
                    FilterMode.NON_FAVORITE_ONLY -> inventory.cards[cards[i]] ?: 0
                }

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