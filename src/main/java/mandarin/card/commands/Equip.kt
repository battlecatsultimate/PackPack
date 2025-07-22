package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.EquipHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import kotlin.math.min

class Equip : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val g = loader.guild

        val inventory = Inventory.getInventory(m.idLong)

        replyToMessageSafely(ch, getText(g, m, inventory), loader.message, { a ->
            a.setComponents(getComponents(g, m, inventory))
        }, { msg ->
            StaticStore.putHolder(m.id, EquipHolder(loader.message, m.id, ch.id, msg, inventory))
        })
    }

    private fun getText(g: Guild, m: Member, inventory: Inventory) : String {
        val builder = StringBuilder("Purchased vanity roles of ${m.asMention}\n\n")

        if (inventory.vanityRoles.isEmpty()) {
            builder.append("- No Roles")
        } else {
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
        }

        return builder.toString()
    }

    private fun getComponents(g: Guild, m: Member, inventory: Inventory) : List<MessageTopLevelComponent> {
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