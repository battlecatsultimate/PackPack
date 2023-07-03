package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Member
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

class CardInventoryHolder(author: Message, channelID: String, private val message: Message, private val inventory: Inventory, private val member: Member) : ComponentHolder(author, channelID, message.id) {
    private val cards = ArrayList<Card>(inventory.cards.keys.sortedWith(CardComparator()))

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = intArrayOf(-1, -1)

    override fun clean() {

    }

    override fun onExpire(id: String?) {
        message.editMessage("Inventory Closed")
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(ArrayList())
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
                page -= 10

                applyResult(event)
            }
            "confirm" -> {
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

                page = 0

                filterCards()

                applyResult(event)
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.size < 1)
                    return

                val value = event.values[0]

                tier = if (value == "all") {
                    CardData.Tier.NONE
                } else {
                    CardData.Tier.values()[value.replace("tier", "").toInt()]
                }

                banner[0] = -1
                banner[1] = -1

                page = 0

                filterCards()

                applyResult(event)
            }
        }
    }

    private fun filterCards() {
        cards.clear()

        if (tier != CardData.Tier.NONE) {
            if (banner[0] == -1) {
                cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier })
            } else {
                cards.addAll(inventory.cards.keys.filter { c -> c.tier == tier && c.unitID in CardData.bannerData[tier.ordinal][banner[1]] })
            }
        } else {
            if (banner[0] == -1) {
                cards.addAll(inventory.cards.keys)
            } else {
                cards.addAll(inventory.cards.keys.filter { c -> c.unitID in CardData.bannerData[banner[0]][banner[1]] })
            }
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

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            tierCategoryElements.add(SelectOption.of(text, "tier${index}"))
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

        val dataSize = inventory.cards.size

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("confirm", "Confirm"))

        rows.add(ActionRow.of(confirmButtons))

        return action.setComponents(rows)
    }

    private fun getText() : String {
        val builder = StringBuilder("Inventory of ${member.asMention}\n\n```md\n")

        if (cards.size > 0) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = inventory.cards[cards[i]] ?: 1

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