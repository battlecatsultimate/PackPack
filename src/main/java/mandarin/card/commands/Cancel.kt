package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.transaction.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Cancel(private val session: TradingSession) : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return

        val index = session.member.indexOf(m.idLong)

        if (index == -1)
            return

        val msg = getRepliedMessageSafely(ch, "Are you sure you want to cancel this trading? Once trade is done, it cannot be undone", getMessage(event)) { a ->
            val components = ArrayList<Button>()

            components.add(Button.success("confirm", "Confirm"))
            components.add(Button.danger("cancel", "Cancel"))

            a.setActionRow(components)
        }

        StaticStore.putHolder(m.id, ConfirmButtonHolder(getMessage(event), msg, ch.id, {
            CardData.sessions.remove(session)
            CardBot.saveCardData()

            TransactionLogger.logTrade(session, TransactionLogger.TradeStatus.CANCELED)

            ch.sendMessage("Trading has been canceled, session is closed now").queue()

            if (ch is ThreadChannel) {
                ch.manager.setLocked(true).queue()
            }
        }, LangID.EN))
    }
}