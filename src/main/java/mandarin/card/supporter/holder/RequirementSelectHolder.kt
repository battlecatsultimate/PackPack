package mandarin.card.supporter.holder

import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.filter.Filter
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class RequirementSelectHolder(author: Message, channelID: String, private val message: Message, private val product: Product, private val inventory: Inventory, private val role: CardData.Role) : ComponentHolder(author, channelID, message.id) {
    private val filters = ArrayList<Filter>()

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "condition" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                filters.clear()

                event.values.forEach {
                    filters.add(product.possibleFilters[it.replace("condition", "").toInt()])
                }

                event.deferEdit()
                    .setContent("Please select requirements that you will use" + if (filters.isNotEmpty()) "\n\n${filters.size} requirement(s) selected" else "")
                    .setComponents(registerComponents())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "confirm" -> {
                val impossibleFilter = filters.find { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } < f.amount }

                if (impossibleFilter != null) {
                    event.deferReply()
                        .setContent("It seems you selected requirement that you can't afford with your inventory, please select other requirements\n\nRequirement causing problem : ${impossibleFilter.name}")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                event.deferEdit()
                    .setContent(getCardText())
                    .setComponents(registerCardComponents())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true

                expire(authorMessage.author.id)

                StaticStore.putHolder(authorMessage.author.id, FilterProcessHolder(authorMessage, channelID, message, product, filters, inventory, role))
            }
            "cancel" -> {
                expired = true

                event.deferEdit()
                    .setContent("Buying canceled")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expire(authorMessage.author.id)
            }
        }
    }

    private fun registerComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        product.possibleFilters.forEachIndexed { index, process ->
            options.add(SelectOption.of(process.name, "condition$index"))
        }

        val processMenu = StringSelectMenu.create("condition")
            .addOptions(options)
            .setPlaceholder("Select ${product.requiredFilter} requirement" + if (product.requiredFilter > 1) "s" else "")
            .setRequiredRange(product.requiredFilter, product.requiredFilter)
            .setDefaultValues(filters.map { f -> product.possibleFilters.indexOf(f) }.map { "condition$it" })
            .build()

        result.add(ActionRow.of(processMenu))

        result.add(ActionRow.of(Button.success("confirm", "Confirm").withDisabled(product.requiredFilter != filters.size), Button.danger("close", "Cancel")))

        return result
    }

    private fun registerCardComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val cards = inventory.cards.keys.filter { c -> filters[0].filter(c) }

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            tierCategoryElements.add(SelectOption.of(text, "tier${index}"))
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        result.add(ActionRow.of(tierCategory.build()))

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all"))

        CardData.bannerCategoryText.forEachIndexed { index, array ->
            array.forEachIndexed { i, a ->
                bannerCategoryElements.add(SelectOption.of(a, "category-$index-$i"))
            }
        }

        val bannerCategory = StringSelectMenu.create("category")
            .addOptions(bannerCategoryElements)
            .setPlaceholder("Filter Cards by Banners")

        result.add(ActionRow.of(bannerCategory.build()))

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

        result.add(ActionRow.of(cardCategory))

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

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.success("confirm", "Confirm").asDisabled(), Button.danger("close", "Cancel")))

        return result
    }

    private fun getCardText() : String {
        val builder = StringBuilder("Please select cards that meets the requirement\n\nCondition : ${filters[0].name}\n\n### Cards\n\n- No Cards Selected\n\n```md\n")

        val cards = inventory.cards.keys.filter { c -> product.possibleFilters[0].filter(c) }.sortedWith(CardComparator())

        for (i in 0 until min(cards.size, SearchHolder.PAGE_CHUNK)) {
            builder.append(i + 1)
                .append(". ")
                .append(cards[i].cardInfo())

            val amount = inventory.cards[cards[i]] ?: 1

            if (amount > 1) {
                builder.append(" x$amount")
            }

            builder.append("\n")
        }

        builder.append("```")

        return builder.toString()
    }
}