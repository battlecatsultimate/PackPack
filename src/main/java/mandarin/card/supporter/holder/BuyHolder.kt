package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.function.Consumer

class BuyHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    private val inventory = Inventory.getInventory(author.author.id)
    private val possibleRoles = CardData.Role.entries.filter { r -> r != CardData.Role.NONE && r !in inventory.vanityRoles }.toList()

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

                if (selectedRole == CardData.Role.LEGEND) {
                    if (inventory.validForLegendCollector()) {
                        inventory.vanityRoles.add(CardData.Role.LEGEND)

                        event.deferEdit()
                            .setContent("Successfully bought the role <@&${selectedRole.id}>! You can always equip or unequip later by calling `${CardBot.globalPrefix}equip` command")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()

                        CardBot.saveCardData()
                    } else {
                        event.deferReply()
                            .setContent("You haven't collected all cards yet, so you can't get this role")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .setEphemeral(true)
                            .queue()
                    }

                    return
                }

                val product = selectedRole.getProduct()

                if (product.requiredFilter != product.possibleFilters.size) {
                    connectTo(event, RequirementSelectHolder(authorMessage, channelID, message, product, inventory, selectedRole))
                } else {
                    connectTo(event, FilterProcessHolder(authorMessage, channelID, message, product, product.possibleFilters.toList(), inventory, selectedRole))
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

                if (product.requiredFilter != product.possibleFilters.size) {
                    connectTo(event, RequirementSelectHolder(authorMessage, channelID, message, product, inventory, reward))
                } else {
                    connectTo(event, FilterProcessHolder(authorMessage, channelID, message, product, product.possibleFilters.toList(), inventory, reward))
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

    override fun onBack() {
        message.editMessage("Please select a list that you want to get")
            .setComponents(registerComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun registerComponents() : List<LayoutComponent> {
        val rows = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (role in possibleRoles) {
            val affordable = if (role == CardData.Role.LEGEND) {
                inventory.validForLegendCollector()
            } else {
                role.getProduct().possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= role.getProduct().requiredFilter
            }

            options.add(SelectOption.of(role.title, role.key).withEmoji(EmojiStore.ABILITY[role.key]).withDescription(if (affordable) "Affordable" else "Cannot Afford"))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(options)
            .setPlaceholder("Select Role")
            .build()

        val restOptions = ArrayList<SelectOption>()

        var affordable = Product.customEmoji.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= Product.customEmoji.requiredFilter

        restOptions.add(SelectOption.of("Custom Emoji", "emoji").withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        affordable = Product.customRole.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= Product.customRole.requiredFilter

        restOptions.add(SelectOption.of("Custom Role", "role").withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        val restMenu = StringSelectMenu.create("rest")
            .addOptions(restOptions)
            .setPlaceholder("Select Other")
            .build()

        rows.add(ActionRow.of(roleMenu))
        rows.add(ActionRow.of(restMenu))

        rows.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return rows
    }
}