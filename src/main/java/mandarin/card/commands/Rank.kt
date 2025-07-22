package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.RankListHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import kotlin.math.ceil
import kotlin.math.min

class Rank : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        Inventory.getInventory(u.idLong)

        val users = CardData.inventories.keys.sortedBy { id ->
            val inventory = Inventory.getInventory(id)

            inventory.catFoods
        }.asReversed()

        val catFoods = users.map { id ->
            val inventory = Inventory.getInventory(id)

            inventory.catFoods
        }

        replyToMessageSafely(loader.channel, getRankList(users, catFoods, u.idLong), loader.message, { a -> a.setComponents(getComponents(users)) }) { msg ->
            StaticStore.putHolder(u.id, RankListHolder(loader.message, u.id, loader.channel.id, msg, users, catFoods, true))
        }
    }

    private fun getRankList(memberList: List<Long>, catFoods: List<Long>, member: Long) : String {
        val rank = memberList.indexOf(member)

        val builder = StringBuilder(
            if (rank != -1) {
                "Your ranking is #${rank + 1} among ${memberList.size} people"
            } else {
                "You aren't listed in ranking list"
            }
        ).append("\n\n")

        val size = min(SearchHolder.PAGE_CHUNK, memberList.size)

        for (m in 0 until size) {
            builder.append(m + 1).append(". <@").append(memberList[m]).append("> : ").append(EmojiStore.ABILITY["CF"]?.formatted).append(" ").append(catFoods[m])

            if (m < size - 1)
                builder.append("\n")
        }

        if (memberList.size > SearchHolder.PAGE_CHUNK) {
            val totalPage = ceil(memberList.size / SearchHolder.PAGE_CHUNK * 1.0).toInt()

            builder.append("\n\n").append("Page : ").append(1).append("/").append(totalPage)
        }

        return builder.toString()
    }

    private fun getComponents(memberList: List<Long>) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        if (memberList.size > SearchHolder.PAGE_CHUNK) {
            val pages = ArrayList<Button>()

            if (memberList.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            pages.add(Button.of(ButtonStyle.SECONDARY,"prev", "Previous Page", EmojiStore.PREVIOUS).asDisabled())
            pages.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (memberList.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(pages))
        }

        result.add(ActionRow.of(Button.danger("close", "Close")))

        return result
    }
}