package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.CardCheckHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Member
import kotlin.math.min

class Check(private val tier: CardData.Tier) : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val g = loader.guild
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val ids = CardData.inventories.keys

        val members = g.findMembers { member -> member.idLong in ids }.get().filter { member ->
            val inventory = Inventory.getInventory(member.idLong)

            inventory.cards.keys.any { c -> c.tier == tier } || inventory.favorites.keys.any { c -> c.tier == tier }
        }.sortedByDescending {
            val inventory = Inventory.getInventory(it.idLong)

            inventory.cards.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value } +
            inventory.favorites.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value }
        }

        if (members.isNotEmpty()) {
            replyToMessageSafely(ch, getText(members), loader.message, { a ->
                a.setComponents(getComponents(members))
            }, { msg ->
                StaticStore.putHolder(m.id, CardCheckHolder(loader.message, m.id, ch.id, msg, members, tier))
            })
        } else {
            replyToMessageSafely(ch, getText(members), loader.message) { a -> a }
        }
    }

    private fun getText(members: List<Member>) : String {
        val builder = StringBuilder()

        if (members.isNotEmpty()) {
            builder.append("Below list is members who have T${tier.ordinal} cards\n\n")

            for (i in 0 until min(members.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                val inventory = Inventory.getInventory(members[i].idLong)

                builder.append(i + 1)
                    .append(". ")
                    .append(members[i].asMention)
                    .append(" [")
                    .append(members[i].id)
                    .append("] x")
                    .append(
                        inventory.cards.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value } +
                        inventory.favorites.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value }
                    )
                    .append("\n")
            }
        } else {
            builder.append("There are no members who have T${tier.ordinal} cards yet")
        }

        return builder.toString()
    }

    private fun getComponents(members: List<Member>) : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val dataSize = members.size

        val totalPage = SearchHolder.getTotalPage(dataSize)

        if (dataSize > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
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