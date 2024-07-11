package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.requests.restaction.CacheRestAction

class PlatinumShard : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val contents = loader.content.split(" ")

        if (contents.size >= 2) {
            val memberGetter = tryToGetMember(contents[1], loader.guild)

            if (memberGetter != null) {
                memberGetter.queue({ mem ->
                    val inventory = Inventory.getInventory(mem.idLong)

                    replyToMessageSafely(loader.channel, "User ${mem.asMention} currently has ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}", loader.message) { a -> a }
                }) { _ ->
                    replyToMessageSafely(loader.channel, "Bot failed to find such user in this server...", loader.message) { a -> a }
                }
            } else {
                replyToMessageSafely(loader.channel, "You have passed invalid format of ID! Please provide member data via either raw ID or mention", loader.message) { a -> a }
            }
        } else {
            val inventory = Inventory.getInventory(m.idLong)

            replyToMessageSafely(loader.channel, "${m.asMention}, you currently have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}", loader.message) { a -> a }
        }
    }

    private fun tryToGetMember(id: String, g: Guild) : CacheRestAction<Member>? {
        val realId = id.replace("<@", "").replace(">", "")

        if (!StaticStore.isNumeric(realId))
            return null

        return g.retrieveMember(UserSnowflake.fromId(realId))
    }
}