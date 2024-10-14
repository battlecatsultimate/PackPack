package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import okhttp3.internal.UTC
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class ChangeAuctionTime : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val segments = loader.content.split(" ")

        if (segments.size < 2) {
            replyToMessageSafely(ch, "Please specify end date that will be replaced into!", loader.message) { a -> a }

            return
        }

        val auctionSession = if (segments.size >= 3) {
            CardData.auctionSessions.find { s -> s.channel == getChannelID(segments[1]) }
        } else {
            CardData.auctionSessions.find { s -> s.channel == ch.idLong }
        }

        if (auctionSession == null) {
            replyToMessageSafely(ch, "Bot failed to find auction session in this channel, or provided channel in the command wasn't recognized or valid", loader.message) { a -> a }

            return
        }

        val dateTime = if (segments.size >= 3) {
            parseDate(segments[2])
        } else {
            parseDate(segments[1])
        }

        if (dateTime * 1000L - CardData.getUnixEpochTime() < 0L) {
            replyToMessageSafely(ch, "You can't change time to older time than current time! Or bot failed to parse the time format\n" +
                    "\n" +
                    "Format is : YYYY-MM-DD-hh-mm-ss (i.e. `2024-5-25-18-30-10`)\n" +
                    "\n" +
                    "Bot always follows UTC time zone", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Are you sure you want to change end date time of auction like below?\n" +
                "### Auction Info\n" +
                "Auction : Auction Session #${auctionSession.id}\n" +
                "Auction Place : <#${auctionSession.channel}> [${auctionSession.channel}]\n" +
                "Auction Message : ${if (auctionSession.getAuctionMessage() != null) auctionSession.getAuctionMessage()?.jumpUrl else "UNKNOWN"}\n" +
                "### End Date\n" +
                "From : <t:${auctionSession.endDate}:f> <t:${auctionSession.endDate}:R>\n" +
                "To : <t:${dateTime}:f> <t:${dateTime}:R>\n" +
                "Time ${if (dateTime - auctionSession.endDate < 0) "Decreased" else "Increased"} By : ${CardData.convertMillisecondsToText(abs((dateTime - auctionSession.endDate) * 1000L))}", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, m.id, ch.id, msg, CommonStatic.Lang.Locale.EN) {
                auctionSession.changeEndTime(m.idLong, dateTime)

                replyToMessageSafely(ch, "Successfully changed end time!", loader.message) { a -> a }
            })
        }
    }

    private fun parseDate(time: String) : Long {
        val segments = time.split("-")

        val date = Calendar.getInstance()

        var year = 0
        var month = 0
        var day = 0
        var hour = 0
        var minute = 0
        var second = 0

        segments.forEachIndexed { index, segment ->
            if (StaticStore.isNumeric(segment)) {
                val value = max(0, StaticStore.safeParseInt(segment))

                when (index) {
                    0 -> year = value
                    1 -> month = min(12, value)
                    2 -> day = when (month) {
                        1, 3, 5, 7, 8, 10, 12 -> min(31, value)
                        4, 6, 9, 11 -> min(30, value)
                        2 -> if (year % 4 == 0)
                            min(29, value)
                        else
                            min(28, value)
                        else -> 0
                    }
                    3 -> hour = min(23, value)
                    4 -> minute = min(59, value)
                    5 -> second = min(59, value)
                }
            }
        }

        date.set(year, max(0, month - 1), day, hour, minute, second)
        date.timeZone = UTC

        return date.timeInMillis / 1000L
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