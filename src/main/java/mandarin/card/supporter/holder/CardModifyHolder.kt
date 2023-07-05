package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class CardModifyHolder(author: Message, channelID: String, private val message: Message, private val isAdd: Boolean, private val inventory: Inventory) : ComponentHolder(author, channelID, message.id) {
    private val cards = ArrayList<Card>(
        if (isAdd) {
            CardData.cards.sortedWith(CardComparator())
        } else {
            inventory.cards.keys.sortedWith(CardComparator())
        }
    )

    private val selectedCards = ArrayList<Card>()

    private var page = 0
    private var tier = CardData.Tier.NONE
    private var banner = intArrayOf(-1, -1)

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
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
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val index = event.values[0].toInt()

                selectedCards.add(cards[index])

                filterCards()

                applyResult(event)
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
                page -= 10

                applyResult(event)
            }
            "confirm" -> {
                selectedCards.forEach {
                    if (isAdd) {
                        inventory.cards[it] = (inventory.cards[it] ?: 0) + 1
                    } else {
                        inventory.cards[it] = (inventory.cards[it] ?: 1) - 1

                        if (inventory.cards[it] == 0) {
                            inventory.cards.remove(it)
                        }
                    }
                }

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
                    repeat(inventory.cards[c] ?: 0) {
                        selectedCards.add(c)
                    }
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

                applyResult()
            }
            "back" -> {
                event.deferEdit()
                    .setContent("Do you want to add cards or remove cards?")
                    .setComponents(getPreviousComponents())
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire(authorMessage.author.id)

                StaticStore.putHolder(authorMessage.author.id, ModifyModeSelectHolder(authorMessage, channelID, message, true, inventory))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire(authorMessage.author.id)
            }
        }
    }

    private fun filterCards() {
        cards.clear()

        val tempCards = if (isAdd) {
            CardData.cards
        } else {
            inventory.cards.keys
        }

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

        if (!isAdd) {
            cards.removeIf { c -> (inventory.cards[c] ?: 1) - selectedCards.filter { card -> c.id == card.id }.size <= 0 }
        }

        cards.sortWith(CardComparator())
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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

    private fun assignComponents() : List<LayoutComponent> {
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

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
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

            for (card in selectedCards.toSet()) {
                checker.append("- ")
                    .append(card.cardInfo())

                val amount = selectedCards.filter { c -> c.id == card.id }.size

                if (amount >= 2) {
                    checker.append(" x$amount")
                }

                checker.append("\n")
            }

            if (checker.length > 1500) {
                builder.append("- ${selectedCards.size} Cards Selected")
            } else {
                builder.append(checker)
            }
        }

        builder.append("\n```md\n")

        if (cards.size > 0) {
            for (i in page * SearchHolder.PAGE_CHUNK until min((page + 1) * SearchHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                val amount = if (isAdd)
                    1
                else
                    (inventory.cards[cards[i]] ?: 1) - selectedCards.filter { c -> c.id == cards[i].id }.size

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

    private fun getPreviousComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Add", "add").withEmoji(Emoji.fromUnicode("➕")))
        modeOptions.add(SelectOption.of("Remove", "remove").withEmoji(Emoji.fromUnicode("➖")))

        val modes = StringSelectMenu.create("mode")
            .addOptions(modeOptions)
            .setPlaceholder("Please select mode")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(
            Button.secondary("back", "Back"),
            Button.danger("close", "Close")
        ))

        return rows
    }
}