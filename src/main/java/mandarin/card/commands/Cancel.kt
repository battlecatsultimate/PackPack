package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Cancel(private val session: TradingSession) : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        val index = session.member.indexOf(m.idLong)

        if (index == -1 && !CardData.isManager(m))
            return

        replyToMessageSafely(ch, "Are you sure you want to cancel this trading? Once trade is done, it cannot be undone", loader.message, { a ->
            val components = ArrayList<Button>()

            components.add(Button.success("confirm", "Confirm"))
            components.add(Button.danger("cancel", "Cancel"))

            a.setActionRow(components)
        }, { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, m.id, ch.id, msg, CommonStatic.Lang.Locale.EN, true) {
                CardData.sessions.remove(session)
                CardBot.saveCardData()

                if (index == -1) {
                    TransactionLogger.logTradeCancel(session, m.idLong)
                } else {
                    TransactionLogger.logTrade(session, TransactionLogger.TradeStatus.CANCELED)
                }

                ch.sendMessage("Trading has been canceled, session is closed now").queue()

                if (ch is ThreadChannel) {
                    ch.manager.setLocked(true).queue()
                }
            })
        })
    }
}