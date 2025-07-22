package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.filter.Filter
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.function.Consumer

class RequirementSelectHolder : ComponentHolder {
    private val product: Product
    private val inventory: Inventory
    private val role: CardData.Role
    private val reward: Consumer<GenericComponentInteractionCreateEvent>

    constructor(author: Message, userID: String, channelID: String, message: Message, product: Product, inventory: Inventory, role: CardData.Role) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.product = product
        this.inventory = inventory
        this.role = role

        reward = Consumer {  }
    }

    constructor(author: Message, userID: String, channelID: String, message: Message, product: Product, inventory: Inventory, reward: Consumer<GenericComponentInteractionCreateEvent>) : super(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
        this.product = product
        this.inventory = inventory
        this.reward = reward

        role = CardData.Role.NONE
    }

    private val filters = ArrayList<Filter>()

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Purchase expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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

                applyResult(event)
            }
            "confirm" -> {
                if (role != CardData.Role.NONE) {
                    connectTo(event, FilterProcessHolder(authorMessage, userID, channelID, message, product, filters, inventory, role))
                } else {
                    connectTo(event, FilterProcessHolder(authorMessage, userID, channelID, message, product, filters, inventory, reward))
                }
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Buying canceled")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
            event.deferEdit()
                .setContent("It seems you can't afford this role with your cards")
                .setAllowedMentions(ArrayList())
                .setComponents(registerComponents())
                .mentionRepliedUser(false)
                .queue()

            return
        } else {
            val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

            if (doableFilters.size < product.requiredFilter) {
                event.deferEdit()
                    .setContent("It seems you can't afford this role with your cards")
                    .setAllowedMentions(ArrayList())
                    .setComponents(registerComponents())
                    .mentionRepliedUser(false)
                    .queue()

                return
            }
        }

        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent("Please select requirements that you will use" + if (filters.isNotEmpty()) "\n\n${filters.size} requirement(s) selected" else "")
            .setComponents(registerComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage("Please select requirements that you will use" + if (filters.isNotEmpty()) "\n\n${filters.size} requirement(s) selected" else "")
            .setComponents(registerComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun registerComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        if (product.requiredFilter == product.possibleFilters.size && product.possibleFilters.any { f -> !f.match(inventory.cards.keys.toList(), inventory)}) {
            result.add(ActionRow.of(Button.secondary("back", "Back")))

            return result
        } else {
            val doableFilters = product.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= f.amount }

            if (doableFilters.size < product.requiredFilter) {
                result.add(ActionRow.of(Button.secondary("back", "Back")))

                return result
            }
        }

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
            .setDefaultValues(filters.map { f -> product.possibleFilters.indexOf(f) }.map { "condition$it" })
            .build()

        result.add(ActionRow.of(processMenu))

        result.add(
            ActionRow.of(
                Button.success("confirm", "Confirm").withDisabled(product.requiredFilter != filters.size),
                Button.secondary("back", "Back"),
                Button.danger("close", "Cancel")
            )
        )

        return result
    }
}