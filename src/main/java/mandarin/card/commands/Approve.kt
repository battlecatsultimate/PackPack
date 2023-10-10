package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.GenericMessageEvent

class Approve : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent) {
        val m = getMember(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val ch = getChannel(event)

        if (ch !is ThreadChannel)
            return

        if (ch.parentChannel.id != if (CardBot.test) CardData.testTradingPlace else CardData.tradingPlace) {
            return
        }

        val session = findSession(ch)

        if (session == null) {
            replyToMessageSafely(ch, "Failed to obtain session data in here", getMessage(event)) { a -> a }

            return
        }

        if (session.approved) {
            replyToMessageSafely(ch, "This session has been approved already!", getMessage(event)) { a -> a }
        } else {
            session.approved = true

            replyToMessageSafely(ch, "Successfully approved the session. Keep in mind that managers will have to re-approve if suggestion gets edited", getMessage(event)) { a -> a }

            TransactionLogger.logApproval(session, m)
        }
    }

    private fun findSession(ch: MessageChannel) : TradingSession? {
        return CardData.sessions.find { s -> s.postID == ch.idLong }
    }
}