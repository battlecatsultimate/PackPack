package mandarin.card.commands

import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.requests.restaction.CacheRestAction

class CatFood : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val contents = loader.content.split(" ")

        val cf = EmojiStore.ABILITY["CF"]?.formatted

        if (contents.size >= 2) {
            val memberGetter = tryToGetMember(contents[1], loader.guild)

            if (memberGetter != null) {
                memberGetter.queue({ mem ->
                    val inventory = Inventory.getInventory(mem.idLong)

                    replyToMessageSafely(loader.channel, "User ${mem.asMention} currently has $cf ${inventory.catFoods}", loader.message) { a -> a }
                }) { _ ->
                    replyToMessageSafely(loader.channel, "Bot failed to find such user in this server...", loader.message) { a -> a }
                }
            } else {
                replyToMessageSafely(loader.channel, "You have passed invalid format of ID! Please provide member data via either raw ID or mention", loader.message) { a -> a }
            }
        } else {
            val inventory = Inventory.getInventory(m.idLong)

            replyToMessageSafely(loader.channel, "${m.asMention}, here's your $cf status\n" +
                    "\n" +
                    "In total, you have $cf ${inventory.catFoods}\n" +
                    "$cf ${inventory.tradePendingCatFoods} are queued in trading sessions\n" +
                    "$cf ${inventory.auctionPendingCatFoods} are queued in auction sessions\n" +
                    "\n" +
                    "You can actually use $cf ${inventory.actualCatFood}", loader.message) { a -> a }
        }
    }

    private fun tryToGetMember(id: String, g: Guild) : CacheRestAction<Member>? {
        val realId = id.replace("<@", "").replace(">", "")

        if (!StaticStore.isNumeric(realId))
            return null

        return g.retrieveMember(UserSnowflake.fromId(realId))
    }
}