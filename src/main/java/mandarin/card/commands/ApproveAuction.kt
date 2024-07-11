package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class ApproveAuction : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val auctionSession = CardData.auctionSessions.find { s -> s.channel == ch.idLong }

        if (auctionSession == null) {
            replyToMessageSafely(ch, "Failed to find auction in this channel. Maybe auction has been ended?", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Are you sure you want to approve this auction?", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, ch.id, CommonStatic.Lang.Locale.EN) {
                val warnMessage = auctionSession.performAuction(m.idLong)

                if (warnMessage.isNotEmpty()) {
                    replyToMessageSafely(ch, warnMessage, loader.message) { a -> a }
                } else {
                    val successMessage = StringBuilder()

                    if (auctionSession.author != -1L)
                        successMessage.append("<@").append(auctionSession.author).append(">, ")

                    successMessage.append("<@").append(auctionSession.getMostBidMember()).append("> Check your inventory")

                    replyToMessageSafely(ch, "Successfully approved the auction, and transaction has been done!", loader.message) { a -> a }

                    ch.sendMessage(successMessage.toString()).queue()
                }
            })
        }
    }
}