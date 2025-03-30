package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.Product
import mandarin.card.supporter.holder.BuyHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Buy : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val author = loader.message

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val inventory = Inventory.getInventory(m.idLong)
        val possibleRoles = CardData.Role.entries.filter { r -> r != CardData.Role.NONE && r !in inventory.vanityRoles }.toList()

        replyToMessageSafely(ch, "Please select a list that you want to get", author, {
            a -> a.setComponents(registerComponents(possibleRoles, inventory))
        }, { msg ->
            StaticStore.putHolder(m.id, BuyHolder(author, m.id, ch.id, msg))
        })
    }

    private fun registerComponents(roles: List<CardData.Role>, inventory: Inventory) : List<LayoutComponent> {
        val rows = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (role in roles) {
            val affordable = when(role) {
                CardData.Role.LEGEND -> inventory.validForLegendCollector()
                CardData.Role.ZAMBONER,
                CardData.Role.WHALELORD -> {
                    val cost = if (role == CardData.Role.ZAMBONER) {
                        1000000
                    } else {
                        5000000
                    }

                    inventory.actualCatFood >= cost
                }
                else -> role.getProduct().possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= role.getProduct().requiredFilter
            }
            options.add(SelectOption.of(role.title, role.key).withEmoji(EmojiStore.ABILITY[role.key]).withDescription(if (affordable) "Affordable" else "Cannot Afford"))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(options)
            .setPlaceholder("Select Role")
            .build()

        val restOptions = ArrayList<SelectOption>()

//        var affordable = Product.customEmoji.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= Product.customEmoji.requiredFilter
//
//        restOptions.add(SelectOption.of("Custom Emoji", "emoji").withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        val affordable = Product.customRole.possibleFilters.filter { f -> inventory.cards.keys.filter { c -> f.filter(c) }.sumOf { c -> inventory.cards[c] ?: 0 } >= f.amount }.size >= Product.customRole.requiredFilter

        restOptions.add(SelectOption.of("Custom Role", "role").withDescription(if (affordable) "Affordable" else "Cannot Afford"))

        val restMenu = StringSelectMenu.create("rest")
            .addOptions(restOptions)
            .setPlaceholder("Select Other")
            .setDisabled(true)
            .build()

        rows.add(ActionRow.of(roleMenu))
        rows.add(ActionRow.of(restMenu))

        rows.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return rows
    }
}