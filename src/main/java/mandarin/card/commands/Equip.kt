package mandarin.card.commands

import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.EquipHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.math.min

class Equip : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return
        val g = getGuild(event) ?: return

        val inventory = Inventory.getInventory(m.id)

        val msg = getRepliedMessageSafely(ch, getText(g, m, inventory), getMessage(event)) {
            a -> a.setComponents(getComponents(g, m, inventory))
        }

        StaticStore.putHolder(m.id, EquipHolder(getMessage(event), ch.id, msg, inventory))
    }

    private fun getText(g: Guild, m: Member, inventory: Inventory) : String {
        val builder = StringBuilder("Purchased vanity roles of ${m.asMention}\n\n")

        for (i in 0 until min(inventory.vanityRoles.size, 3)) {
            val r = g.roles.find { r -> r.id == inventory.vanityRoles[i].id }

            if (r == null) {
                builder.append("- ").append(inventory.vanityRoles[i].title)
            } else {
                val equipped = if (m.roles.contains(r)) {
                    "Equipped"
                } else {
                    "Unequipped"
                }

                builder.append("- ")
                    .append(inventory.vanityRoles[i].title)
                    .append(" : ")
                    .append(equipped)
            }

            builder.append("\n")
        }

        return builder.toString()
    }

    private fun getComponents(g: Guild, m: Member, inventory: Inventory) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val roles = inventory.vanityRoles

        for (i in 0 until min(roles.size, 3)) {
            val r = g.roles.find { r -> r.id == roles[i].id }

            if (r == null) {
                rows.add(ActionRow.of(Button.secondary(roles[i].title, "role/${roles[i].name}").withEmoji(EmojiStore.ABILITY[roles[i].id])))
            } else {
                val equipped = if (m.roles.contains(r)) {
                    "Equipped"
                } else {
                    "Unequipped"
                }

                rows.add(ActionRow.of(Button.secondary("role/${roles[i].name}", "${roles[i].title} : $equipped").withEmoji(EmojiStore.ABILITY[roles[i].key])))
            }
        }
        val dataSize = roles.size

        var totPage = dataSize / 3

        if (dataSize % 3 != 0)
            totPage++

        if (dataSize > 3) {
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

        confirmButtons.add(Button.primary("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }
}