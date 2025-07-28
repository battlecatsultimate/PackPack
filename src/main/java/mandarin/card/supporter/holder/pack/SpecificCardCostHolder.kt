package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.holder.skin.SkinCostManageHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SpecificCardCostHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val pack: CardPack,
    private val cost: SpecificCardCost,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val cards = ArrayList<Card>(CardData.cards.sortedWith(CardComparator()))

    private var page = 0

    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack manager expired")
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
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardCostAmountHolder(authorMessage, userID, channelID, message, cost))
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

                goBackTo(SkinCostManageHolder::class.java)
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone"
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBackTo(e, CardPackCostHolder::class.java)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card cost? This can't be undone"
                )

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    pack.cost.cardsCosts.remove(cost)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
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

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
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
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}\n")
            }
        } else {
            builder.append("No Cards Found")
        }

        builder.append("```")

        return builder.toString()
    }

    private fun assignComponents() : List<MessageTopLevelComponent> {
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
            for(i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(dataSize, (page + 1 ) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
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

        val totalPage = getTotalPage(dataSize)

        if (dataSize > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10)
                buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))

            buttons.add(Button.secondary("prev", "Previous Pages").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.secondary("amount", "Set Amount of Cards"))

            buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if(totalPage > 10) {
                buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
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