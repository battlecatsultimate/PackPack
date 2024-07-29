package mandarin.card.supporter.holder.modal.auction

import common.CommonStatic
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import kotlin.collections.forEach
import kotlin.text.contains
import kotlin.text.count
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.split

class AuctionAutoCloseHolder(author: Message, channelID: String, message: Message, private val onSelected: (Long) -> Unit) : ModalHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun clean() {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "autoClose")
            return

        val value = getValueFromMap(event.values, "time")

        if (!StaticStore.isNumeric(value)) {
            val time = parseTime(value)

            if (time == -1L) {
                event.deferReply()
                    .setContent("Auto close time must be numeric or have valid pattern!")
                    .setEphemeral(true)
                    .queue()

                goBack()

                return
            }

            onSelected.invoke(time)

            event.deferReply()
                .setContent("Successfully changed auto close time of this auction! Check result above")
                .setEphemeral(true)
                .queue()

            goBack()
        } else {
            val time = StaticStore.safeParseLong(value)

            if (time < 0) {
                event.deferReply()
                    .setContent("Auto close time must be 0 or positive!")
                    .setEphemeral(true)
                    .queue()

                goBack()

                return
            }

            onSelected.invoke(time * 1000)

            event.deferReply()
                .setContent("Successfully changed auto close time of this auction! Check result above")
                .setEphemeral(true)
                .queue()

            goBack()
        }
    }

    private fun parseTime(text: String) : Long {
        var filtered = text.replace(Regex("\\s"), "").lowercase()

        if (!StaticStore.isNumeric(filtered.replace(Regex("[hmds]"), ""))) {
            return -1
        }

        arrayOf('d', 'h', 'm', 's').forEach { l ->
            if (filtered.count { letter -> letter == l } > 1)
                return -1
        }

        var time = 0L

        if (filtered.contains("d")) {
            val split = filtered.split(Regex("d"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val day = StaticStore.safeParseLong(split[0])

            time += day * 24 * 60 * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("h")) {
            val split = filtered.split(Regex("h"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val hour = StaticStore.safeParseLong(split[0])

            time += hour * 60 * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("m")) {
            val split = filtered.split(Regex("m"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val minute = StaticStore.safeParseLong(split[0])

            time += minute * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("s")) {
            val split = filtered.split(Regex("s"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val second = StaticStore.safeParseLong(split[0])

            time += second * 1000
        }

        return time
    }
}