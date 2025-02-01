package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.requests.restaction.CacheRestAction

class Balance : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user
        val contents = loader.content.split(" ")

        val cf = EmojiStore.ABILITY["CF"]?.formatted
        val ps = EmojiStore.ABILITY["SHARD"]?.formatted

        if (contents.size >= 2) {
            val memberGetter = tryToGetMember(contents[1], loader.guild)

            if (memberGetter != null) {
                memberGetter.queue({ mem ->
                    val inventory = Inventory.getInventory(mem.idLong)
                    val cfAmount = inventory.catFoods - (CardData.taxedCatFoods[mem.idLong] ?: 0)

                    replyToMessageSafely(loader.channel, "User ${mem.asMention}'s balance status : \n\n- **Cat Food** : $cf $cfAmount\n\n- **Platinum Shard** : $ps ${inventory.platinumShard}", loader.message) { a -> a }
                }) { _ ->
                    replyToMessageSafely(loader.channel, "Bot failed to find such user in this server...", loader.message) { a -> a }
                }
            } else {
                replyToMessageSafely(loader.channel, "You have passed invalid format of ID! Please provide member data via either raw ID or mention", loader.message) { a -> a }
            }
        } else {
            val inventory = Inventory.getInventory(u.idLong)

            if (CardData.taxedCatFoods.containsKey(u.idLong)) {
                replyToMessageSafely(loader.channel, "${u.asMention}, here's your balance status\n" +
                        "\n" +
                        "- **For Cat Foods**\n" +
                        "In total, you have $cf ${inventory.catFoods}\n" +
                        "$cf ${inventory.tradePendingCatFoods} are queued in trading sessions\n" +
                        "$cf ${inventory.auctionPendingCatFoods} are queued in auction sessions\n" +
                        "$cf ${CardData.taxedCatFoods[u.idLong]} are taxed\n" +
                        "\n" +
                        "You can actually use $cf ${inventory.actualFakeCatFood}\n" +
                        "\n" +
                        "- **For Platinum Shards**\n" +
                        "You currently have $ps ${inventory.platinumShard}", loader.message) { a -> a }
            } else {
                replyToMessageSafely(loader.channel, "${u.asMention}, here's your balance status\n" +
                        "\n" +
                        "- **For Cat Foods**\n" +
                        "In total, you have $cf ${inventory.catFoods}\n" +
                        "$cf ${inventory.tradePendingCatFoods} are queued in trading sessions\n" +
                        "$cf ${inventory.auctionPendingCatFoods} are queued in auction sessions\n" +
                        "\n" +
                        "You can actually use $cf ${inventory.actualCatFood}\n" +
                        "\n" +
                        "- **For Platinum Shards**\n" +
                        "You currently have $ps ${inventory.platinumShard}", loader.message) { a -> a }
            }
        }
    }

    private fun tryToGetMember(id: String, g: Guild) : CacheRestAction<Member>? {
        val realId = id.replace("<@", "").replace(">", "")

        if (!StaticStore.isNumeric(realId))
            return null

        return g.retrieveMember(UserSnowflake.fromId(realId))
    }
}