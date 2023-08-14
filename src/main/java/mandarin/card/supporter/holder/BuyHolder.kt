package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.transaction.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.function.Consumer
import kotlin.math.min

class BuyHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val m = event.member ?: return

        val inventory = Inventory.getInventory(m.id)

        when(event.componentId) {
            "role" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val value = event.values[0]

                val selectedRole = CardData.Role.entries.find { r -> r.key == value } ?: return

                expired = true

                expire(authorMessage.author.id)

                if (selectedRole == CardData.Role.LEGEND) {
                    val cards = inventory.cards.keys.map { c -> c.unitID }

                    val possible = (0..1).any { i -> CardData.permanents[i].any { b -> cards.containsAll(CardData.bannerData[i][b].toList()) && cards.contains(CardData.regularLegend[i * 9 + b]) } }

                    if (possible) {
                        inventory.vanityRoles.add(CardData.Role.LEGEND)

                        event.deferEdit()
                            .setContent("Successfully bought the role <@&${selectedRole.id}>! You can always equip or unequip later by calling `${CardBot.globalPrefix}equip` command")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()

                        CardBot.saveCardData()
                    } else {
                        event.deferEdit()
                            .setContent("You haven't collected all cards yet, so you can't get this role")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()
                    }

                    return
                }

                val product = selectedRole.getProduct()

                if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
                    event.deferEdit()
                        .setContent("It seems you can't afford this role with your cards")
                        .setAllowedMentions(ArrayList())
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue()

                    return
                } else {
                    val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

                    if (doableFilters.size < product.requiredFilter) {
                        event.deferEdit()
                            .setContent("It seems you can't afford this role with your cards")
                            .setAllowedMentions(ArrayList())
                            .setComponents()
                            .mentionRepliedUser(false)
                            .queue()

                        return
                    }
                }

                event.deferEdit()
                    .setContent(getText(product, inventory))
                    .setComponents(registerComponents(product, inventory))
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                if (product.requiredFilter != product.possibleFilters.size) {
                    StaticStore.putHolder(authorMessage.author.id, RequirementSelectHolder(authorMessage, channelID, message, product, inventory, selectedRole))
                } else {
                    StaticStore.putHolder(authorMessage.author.id, FilterProcessHolder(authorMessage, channelID, message, product, product.possibleFilters.toList(), inventory, selectedRole))
                }
            }
            "rest" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val product = when(event.values[0]) {
                    "emoji" -> Product.customEmoji
                    "role" -> Product.customRole
                    else -> throw IllegalStateException("Invalid product name ${event.values[0]}")
                }

                if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
                    event.deferEdit()
                        .setContent("It seems you can't afford this item with your cards")
                        .setAllowedMentions(ArrayList())
                        .setComponents()
                        .mentionRepliedUser(false)
                        .queue()

                    return
                } else {
                    val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

                    if (doableFilters.size < product.requiredFilter) {
                        event.deferEdit()
                            .setContent("It seems you can't afford this item with your cards")
                            .setAllowedMentions(ArrayList())
                            .setComponents()
                            .mentionRepliedUser(false)
                            .queue()

                        return
                    }
                }

                val itemName = when(event.values[0]) {
                    "250cf" -> "cat foods"
                    "1cf" -> "cat foods"
                    "emoji" -> "custom emoji"
                    "role" -> "custom role"
                    else -> throw IllegalStateException("Invalid product name ${event.values[0]}")
                }

                val amount = when(event.values[0]) {
                    "250cf" -> 250000
                    "1cf" -> 1000000
                    "emoji",
                    "role" -> 1
                    else -> throw IllegalStateException("Invalid product name ${event.values[0]}")
                }

                val reward = Consumer<GenericComponentInteractionCreateEvent> {
                    TransactionLogger.logItemPurchase(authorMessage.author.idLong, itemName, amount)

                    it.deferEdit()
                        .setContent("Successfully purchased item! Please wait for moderators to transfer, or contact them manually")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                }

                event.deferEdit()
                    .setContent(getText(product, inventory))
                    .setComponents(registerComponents(product, inventory))
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                expired = true

                expire(authorMessage.author.id)

                if (product.requiredFilter != product.possibleFilters.size) {
                    StaticStore.putHolder(authorMessage.author.id, RequirementSelectHolder(authorMessage, channelID, message, product, inventory, reward))
                } else {
                    StaticStore.putHolder(authorMessage.author.id, FilterProcessHolder(authorMessage, channelID, message, product, product.possibleFilters.toList(), inventory, reward))
                }
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

    private fun registerComponents(product: Product, inventory: Inventory) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (product.requiredFilter == product.possibleFilters.size) {
            val cards = inventory.cards.keys.filter { c -> product.possibleFilters[0].filter(c) }.sortedWith(CardComparator())

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
        } else {
            val options = ArrayList<SelectOption>()

            product.possibleFilters.forEachIndexed { index, process ->
                val possibleCards = inventory.cards.keys.filter { c -> process.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 }

                val desc = if (possibleCards < 2) {
                    "$possibleCards Card Available"
                } else {
                    "$possibleCards Cards Available"
                }

                options.add(SelectOption.of(process.name, "condition$index").withDescription(desc))
            }

            val processMenu = StringSelectMenu.create("condition")
                .addOptions(options)
                .setPlaceholder("Select ${product.requiredFilter} requirement" + if (product.requiredFilter > 1) "s" else "")
                .setRequiredRange(product.requiredFilter, product.requiredFilter)
                .build()

            result.add(ActionRow.of(processMenu))
        }

        result.add(ActionRow.of(Button.success("confirm", "Confirm").asDisabled(), Button.danger("cancel", "Cancel")))

        return result
    }

    private fun getText(product: Product, inventory: Inventory) : String {
        return if (product.requiredFilter == product.possibleFilters.size) {
            val builder = StringBuilder("Please select cards that meets the requirement\n\nCondition : ${product.possibleFilters[0].name}\n\n### Cards\n\n- No Cards Selected\n\n```md\n")

            val cards = inventory.cards.keys.filter { c -> product.possibleFilters[0].filter(c) }.sortedWith(CardComparator())

            if (cards.isNotEmpty()) {
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
            } else {
                builder.append("No cards to select")
            }

            builder.append("```")

            builder.toString()
        } else {
            "Please select requirements that you will use"
        }
    }
}