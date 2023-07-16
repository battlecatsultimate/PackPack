package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import kotlin.math.min

class CardSelectHolder(author: Message, channelID: String, messageID: String, private val onSelect: (List<Card>, GenericComponentInteractionCreateEvent) -> Unit) : ComponentHolder(author, channelID, messageID) {
    private val inventory = Inventory.getInventory(author.author.id)
    private val cards = ArrayList<Card>(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.UNCOMMON }.sortedWith(CardComparator()))

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
            "select" -> {
                inventory.removeCards(selectedCard)

                onSelect(selectedCard, event)

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
        }
    }

    private fun filterCards() {
        cards.clear()

        if (banner[0] == -1) {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.UNCOMMON }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        } else {
            cards.addAll(inventory.cards.keys.filter { c -> c.tier == CardData.Tier.UNCOMMON && c.unitID in CardData.bannerData[CardData.Tier.UNCOMMON.ordinal][banner[1]] }.filter { c -> (inventory.cards[c] ?: 0) - selectedCard.filter { card -> card.unitID == c.unitID}.size > 0 })
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        assignComponents(
            event.deferEdit()
                .setContent(getText())
                .mentionRepliedUser(false)
                .setAllowedMentions(ArrayList())
        ).queue()
    }

    private fun assignComponents(action: MessageEditCallbackAction) : MessageEditCallbackAction {
        val rows = ArrayList<ActionRow>()

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        CardData.bannerCategoryText[CardData.Tier.UNCOMMON.ordinal].forEachIndexed { i, a ->
            bannerCategoryElements.add(SelectOption.of(a, "category-${CardData.Tier.UNCOMMON.ordinal}-$i"))
        }

        val bannerCategory = StringSelectMenu.create("category")
            .addOptions(bannerCategoryElements)
            .setPlaceholder("Filter Cards by Banners")

        val option = bannerCategoryElements.find { e -> e.value == "category-${CardData.Tier.UNCOMMON.ordinal}-${banner[1]}" }

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
                else if (selectedCard.size == 5)
                    "Selected 5 cards"
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty() || selectedCard.size == 5)
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

        confirmButtons.add(Button.primary("select", "Select").withDisabled(selectedCard.size < 5))
        confirmButtons.add(Button.danger("cancel", "Cancel"))

        rows.add(ActionRow.of(confirmButtons))

        return action.setComponents(rows)
    }

    private fun getText() : String {
        val builder = StringBuilder("Select 5 Tier 2 [Uncommon] cards\n\n### Selected Cards\n\n")

        if (selectedCard.isNotEmpty()) {
            for (c in selectedCard) {
                builder.append("- ")
                    .append(c.cardInfo())
                    .append("\n")
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