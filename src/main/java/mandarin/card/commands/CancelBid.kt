package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.AuctionSession
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel

class CancelBid : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user
        val ch = loader.channel

        val segments = loader.content.split(Regex(" "), 3)

        if (ch is PrivateChannel && segments.size < 2) {
            replyToMessageSafely(ch, "Please provide which auction you will cancel the bid! Format is `${CardBot.globalPrefix}cancelbid [Auction Channel ID]`", loader.message) { a -> a }

            return
        }

        if (ch !is PrivateChannel && ch.idLong !in CardData.auctionPlaces) {
            return
        }

        val auctionSession = if (ch is PrivateChannel) {
            val id = getChannelID(segments[1])

            if (id == -1L) {
                replyToMessageSafely(ch, "It seems you have passed invalid channel format. Channel ID must be passed as raw number or mention", loader.message) { a -> a }

                return
            }

            CardData.auctionSessions.find { s -> s.channel == id }
        } else {
            CardData.auctionSessions.find { s -> s.channel == ch.idLong }
        }

        if (auctionSession == null) {
            if (ch is PrivateChannel) {
                replyToMessageSafely(ch, "Failed to find auction from provided channel. Maybe incorrect ID or auction has been closed already?", loader.message) { a -> a }
            }

            return
        }

        if (auctionSession.author == u.idLong) {
            if (ch is PrivateChannel || !auctionSession.anonymous) {
                replyToMessageSafely(ch, "You can't participate in your auction!", loader.message) { a -> a }
            }

            return
        }

        if (auctionSession.anonymous && ch !is PrivateChannel) {
            loader.message.delete().queue {
                replyToMessageSafely(ch, "This auction is anonymous auction! Users shall not see who bid this auction. Please call this command via DM", loader.message) { a -> a }
            }

            return
        }

        val rank = calculateRank(u.idLong, auctionSession)

        if (rank == -1) {
            replyToMessageSafely(ch, "It seems you haven't bid to this auction", loader.message) { a -> a }

            return
        }

        if (rank < 4) {
            replyToMessageSafely(ch, "Currently your bid is ranked as #${rank + 1}. To prevent abuse, you can't cancel until your bid takes 4th or lower place in the bid list", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Are you sure you want to cancel the bid?", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(u.id, ConfirmButtonHolder(loader.message, u.id, ch.id, msg, CommonStatic.Lang.Locale.EN) {
                auctionSession.cancelBid(u.idLong)

                replyToMessageSafely(ch, "Successfully canceled the bid!", loader.message) { a -> a }
            })
        }
    }

    private fun calculateRank(id: Long, auctionSession: AuctionSession) : Int {
        if (!auctionSession.bidData.containsKey(id))
            return -1

        return auctionSession.bidData.entries.sortedBy { e -> e.value }.indexOfFirst { e -> e.key == id }
    }

    private fun getChannelID(value: String) : Long {
        return if (StaticStore.isNumeric(value)) {
            StaticStore.safeParseLong(value)
        } else {
            val filteredValue = value.replace("<#", "").replace(">", "")

            if (!StaticStore.isNumeric(filteredValue)) {
                return -1L
            }

            StaticStore.safeParseLong(filteredValue)
        }
    }
}