package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji

class SlotContent(private val emojiName: String, private val emojiID: Long) {
    enum class Mode {
        FLAT,
        PERCENTAGE
    }

    companion object {
        fun fromJson(obj: JsonObject) : SlotContent {
            if (!StaticStore.hasAllTag(obj,"emojiName", "emojiID", "mode", "amount")) {
                throw IllegalStateException("E/SlotContent::fromJson - Invalid data found")
            }

            val content = SlotContent(obj.get("emojiName").asString, obj.get("emojiID").asLong)

            content.mode = Mode.valueOf(obj.get("mode").asString)
            content.amount = obj.get("amount").asLong

            return content
        }
    }

    lateinit var emoji: Emoji

    var mode = Mode.PERCENTAGE
    var amount = 0L

    fun load() {
        val e = SlotEmojiContainer.loadedEmoji.filter { emote ->
            if (emote.name != emojiName)
                return@filter false

            return@filter when(emote.type) {
                Emoji.Type.UNICODE -> false
                Emoji.Type.CUSTOM -> (emote as CustomEmoji).idLong == emojiID
            }
        }

        if (e.isEmpty())
            return

        emoji = e.first()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("emojiName", emojiName)
        obj.addProperty("emojiID", emojiID)

        obj.addProperty("mode", mode.name)
        obj.addProperty("amount", amount)

        return obj
    }
}