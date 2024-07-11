package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class RemoveAuctionPlace : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val segments = loader.content.split(Regex(" "), 2)

        if (segments.size < 2) {
            replyToMessageSafely(ch, "You have to provide either mention or ID of the channel that you want to add!", loader.message) { a -> a }

            return
        }

        val targetChannel = retrieveChannel(loader.guild, segments[1])

        if (targetChannel == null) {
            replyToMessageSafely(ch, "It seems bot has failed to retrieve provided channel. Maybe channel is invisible from the bot, or you have passed invalid ID or mention", loader.message) { a -> a }

            return
        }

        if (CardData.auctionSessions.any { s -> s.channel == targetChannel.idLong }) {
            replyToMessageSafely(ch, "This channel has on-going auction currently. To remove it from auction place, you have to end the auction, or cancel it!", loader.message) { a -> a }

            return
        }

        CardData.auctionPlaces.remove(targetChannel.idLong)

        CardBot.saveCardData()

        replyToMessageSafely(ch, "Successfully removed <#${targetChannel.idLong}> [${targetChannel.idLong}] from list of auction places!", loader.message) { a -> a }
    }

    private fun retrieveChannel(g: Guild, id: String) : GuildMessageChannel? {
        val actualId = if (StaticStore.isNumeric(id))
            id
        else {
            val filteredId = id.replace("<#", "").replace(">", "")

            if (!StaticStore.isNumeric(filteredId))
                return null

            filteredId
        }

        val ch = g.getGuildChannelById(actualId)

        return if (ch is GuildMessageChannel)
            ch
        else
            null
    }
}