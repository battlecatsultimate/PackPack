package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class CancelAuction : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val auctionSession = CardData.auctionSessions.find { s -> s.channel == loader.channel.idLong }

        if (auctionSession == null) {
            replyToMessageSafely(loader.channel, "Failed to find on-going auction in this channel. Maybe it's closed already or here isn't auction place?", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(loader.channel, "Are you sure you want to cancel this auction? This process cannot be undone\n\n**__This command doesn't perform transaction. If you want to close the auction AND perform the transaction, please call `${CardBot.globalPrefix}closeauction`__**", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, m.id, loader.channel.id, msg, CommonStatic.Lang.Locale.EN) {
                auctionSession.cancelSession(m.idLong)

                replyToMessageSafely(loader.channel, "Successfully canceled the auction #${auctionSession.id}!", loader.message) { a -> a }
            })
        }
    }
}