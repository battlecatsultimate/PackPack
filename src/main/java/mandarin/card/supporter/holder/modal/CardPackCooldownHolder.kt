package mandarin.card.supporter.holder.modal

import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.modal.ModalHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

class CardPackCooldownHolder(author: Message, channelID: String, messageID: String, private val pack: CardPack) : ModalHolder(author, channelID, messageID) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: ModalInteractionEvent) {
        if (event.modalId != "cooldown")
            return

        val value = getValueFromMap(event.values, "cooldown")

        if (!StaticStore.isNumeric(value)) {
            val time = parseTime(value)

            if (time == -1L) {
                event.deferReply()
                    .setContent("Cooldown value must be numeric or have valid pattern!")
                    .setEphemeral(true)
                    .queue()

                goBack()

                return
            }

            pack.cooldown = time

            event.deferReply()
                .setContent("Successfully changed cooldown of this card pack! Check result above")
                .setEphemeral(true)
                .queue()

            goBack()
        } else {
            val cooldown = value.toDouble().toLong()

            if (cooldown < 0) {
                event.deferReply()
                    .setContent("Cooldown value must be 0 or positive!")
                    .setEphemeral(true)
                    .queue()

                goBack()

                return
            }

            pack.cooldown = cooldown * 1000

            event.deferReply()
                .setContent("Successfully changed cooldown of this card pack! Check result above")
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

            val day = split[0].toDouble().toLong()

            time += day * 24 * 60 * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("h")) {
            val split = filtered.split(Regex("h"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val hour = split[0].toDouble().toLong()

            time += hour * 60 * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("m")) {
            val split = filtered.split(Regex("m"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val minute = split[0].toDouble().toLong()

            time += minute * 60 * 1000

            filtered = split[1]
        }

        if (filtered.contains("s")) {
            val split = filtered.split(Regex("s"), 2)

            if (split.size != 2)
                return -1

            if (!StaticStore.isNumeric(split[0]))
                return -1

            val second = split[0].toDouble().toLong()

            time += second * 1000
        }

        return time
    }
}