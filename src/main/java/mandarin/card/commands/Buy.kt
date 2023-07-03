package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.holder.BuyHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Buy : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return
        val author = getMessage(event) ?: return

        val inventory = Inventory.getInventory(m.id)
        val possibleRoles = CardData.Role.values().filter { r -> r != CardData.Role.NONE && r !in inventory.vanityRoles }.toList()

        val msg = getRepliedMessageSafely(ch, "Please select a list that you want to get", author) {
            a -> a.setComponents(registerComponents(possibleRoles, inventory))
        }

        StaticStore.putHolder(m.id, BuyHolder(author, ch.id, msg))
    }

    private fun registerComponents(roles: List<CardData.Role>, inventory: Inventory) : List<LayoutComponent> {
        val rows = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (role in roles) {
            val affordable = if (role == CardData.Role.LEGEND) {
                val cards = inventory.cards.keys.map { c -> c.unitID }

                CardData.permanents.withIndex().any { v ->
                    v.value.any { index ->
                        CardData.bannerData[v.index][index].any { unit ->
                            unit !in cards
                        }
                    }
                } || CardData.regularLegend.any { it !in cards }
            } else {
                role.getProduct().possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } > f.amount }.size >= role.getProduct().requiredFilter
            }

            options.add(SelectOption.of(role.title, role.key).withEmoji(EmojiStore.ABILITY[role.key]).withDescription(if (affordable) "Affordable" else "Cannot Afford"))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(options)
            .setPlaceholder("Select Role")
            .build()

        val restOptions = ArrayList<SelectOption>()

        var affordable = Product.cf250.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } > f.amount }.size >= Product.cf250.requiredFilter

        restOptions.add(SelectOption.of("250K Cat Foods", "250cf").withEmoji(EmojiStore.ABILITY["CF"]).withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        affordable = Product.cf1m.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } > f.amount }.size >= Product.cf1m.requiredFilter

        restOptions.add(SelectOption.of("1M Cat Foods", "1cf").withEmoji(EmojiStore.ABILITY["CF"]).withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        affordable = Product.customEmoji.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } > f.amount }.size >= Product.customEmoji.requiredFilter

        restOptions.add(SelectOption.of("Custom Emoji", "emoji").withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        affordable = Product.customRole.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } > f.amount }.size >= Product.customRole.requiredFilter

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