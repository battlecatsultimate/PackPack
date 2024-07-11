package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class CloseAuction : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val auctionSession = CardData.auctionSessions.find { s -> s.channel == loader.channel.idLong }

        if (auctionSession == null) {
            replyToMessageSafely(loader.channel, "Failed to find on-going auction in this channel. Maybe it's closed already or here isn't auction place?", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(loader.channel, "Are you sure you want to close this auction? This process cannot be undone\n\n**__This command performs transaction. If you want to close the auction AND NOT perform the transaction, please call `${CardBot.globalPrefix}cancelauction`__**", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, loader.channel.id, CommonStatic.Lang.Locale.EN) {
                val warnMessage = auctionSession.closeSession(m.idLong, false)

                if (warnMessage.isNotEmpty()) {
                    replyToMessageSafely(ch, warnMessage, loader.message) { a -> a }
                } else {
                    replyToMessageSafely(loader.channel, "Successfully closed the auction #${auctionSession.id}, and performed the transaction!", loader.message) { a -> a }

                    val successMessage = StringBuilder()

                    if (auctionSession.author != -1L)
                        successMessage.append("<@").append(auctionSession.author).append(">, ")

                    successMessage.append("<@").append(auctionSession.getMostBidMember()).append("> Check your inventory")

                    ch.sendMessage(successMessage.toString()).queue()
                }
            })
        }
    }
}