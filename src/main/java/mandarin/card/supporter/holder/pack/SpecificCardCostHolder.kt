package mandarin.card.supporter.holder.pack

import mandarin.card.CardBot
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.min

class SpecificCardCostHolder(
    author: Message,
    channelID: String,
    message: Message,
    private val pack: CardPack,
    private val cost: SpecificCardCost,
    private val new: Boolean
) : ComponentHolder(author, channelID, message) {
    private val cards = ArrayList<Card>(CardData.cards.sortedWith(CardComparator()))

    private var page = 0

    private var tier = CardData.Tier.NONE
    private var banner = intArrayOf(-1, -1)

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

                val remove = card in cost.cards

                if (remove) {
                    cost.cards.remove(card)
                } else {
                    cost.cards.add(card)
                }

                event.deferReply()
                    .setContent(
                        if (remove) {
                            "Successfully removed card from required card list! Check the result above"
                        } else {
                            "Successfully added card into required card list! Check the result above"
                        }
                    )
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of required cards")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("amount", "Required Cards Amount")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardCostAmountHolder(authorMessage, channelID, message, cost))
            }
            "create" -> {
                pack.cost.cardsCosts.add(cost)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card cost to this card pack!")
                    .setEphemeral(true)
                    .queue()

                expired = true

                parent?.goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone",
                        LangID.EN
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        expired = true

                        e.deferEdit().queue()

                        parent?.goBack()
                    }, LangID.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    expired = true

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card cost? This can't be undone",
                    LangID.EN
                )

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    pack.cost.cardsCosts.remove(cost)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, LangID.EN))
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

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
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
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select card to change required card of this cost\n\n")

        builder.append("### Selected Card : ").append("\n")

        if (cost.cards.isEmpty()) {
            builder.append("- No Cards")
        } else {
            cost.cards.forEach { c ->
                builder.append("- ").append(c.simpleCardInfo()).append("\n")
            }
        }

        builder.append("### Required Amount : ").append(cost.amount).append("\n\n")

        builder.append("\n```md\n")

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

    private fun assignComponents() : List<LayoutComponent> {
        val result = ArrayList<ActionRow>()

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

        result.add(ActionRow.of(tierCategory.build()))

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

        result.add(ActionRow.of(bannerCategory.build()))

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

        result.add(ActionRow.of(cardCategory))


        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))

            buttons.add(Button.secondary("prev", "Previous Pages").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.secondary("amount", "Set Amount of Cards"))

            buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totPage))

            if(totPage > 10) {
                buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totPage))
            }

            result.add(ActionRow.of(buttons))
        }

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create").withDisabled(cost.cards.isEmpty()),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back").withDisabled(cost.cards.isEmpty()),
                    Button.danger("delete", "Delete Cost")
                )
            )
        }

        return result
    }
}