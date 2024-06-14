package mandarin.card.supporter.holder.modal.auction

import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import okhttp3.internal.UTC
import java.util.Calendar
import kotlin.collections.forEachIndexed
import kotlin.math.max
import kotlin.math.min
import kotlin.text.split

class AuctionEndTimeHolder(author: Message, channelID: String, message: Message, private val onSelected: (timeStamp: Long) -> Unit) : ModalHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "endTime")
            return

        val time = getValueFromMap(event.values, "time")

        val timeStamp = parseDate(time)

        if (timeStamp < CardData.getUnixEpochTime() / 1000L) {
            event.deferReply()
                .setContent("You can't provide time that already has passed! Maybe you didn't consider time zone? Bot accepts time value with UTC time zone")
                .setEphemeral(true)
                .queue()

            return
        }

        onSelected.invoke(timeStamp)

        event.deferReply().setContent("Successfully set end time like above!").setEphemeral(true).queue()

        expired = true

        expire()
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
}