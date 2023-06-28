package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
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
            a -> a.setComponents(registerComponents(possibleRoles))
        }

        StaticStore.putHolder(m.id, BuyHolder(author, ch.id, msg))
    }

    private fun registerComponents(roles: List<CardData.Role>) : List<LayoutComponent> {
        val rows = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (role in roles) {
            options.add(SelectOption.of(role.title, role.key).withEmoji(EmojiStore.ABILITY[role.key]))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(options)
            .setPlaceholder("Select Role")
            .build()

        val restOptions = ArrayList<SelectOption>()

        restOptions.add(SelectOption.of("250K Cat Foods", "250cf").withEmoji(EmojiStore.ABILITY["CF"]))
        restOptions.add(SelectOption.of("1M Cat Foods", "1cf").withEmoji(EmojiStore.ABILITY["CF"]))
        restOptions.add(SelectOption.of("Custom Emoji", "emoji"))
        restOptions.add(SelectOption.of("Custom Role", "role"))

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