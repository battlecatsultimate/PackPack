package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji

abstract class SlotContent(private var emojiName: String, private var emojiID: Long, private val rewardType: RewardType) {
    enum class RewardType {
        CURRENCY,
        CARD
    }

    companion object {
        fun fromJson(obj: JsonObject) : SlotContent {
            if (!StaticStore.hasAllTag(obj, "emojiName", "emojiID", "rewardType")) {
                throw IllegalStateException("E/SlotContent::fromJson - Got invalid json data")
            }

            val rewardType = RewardType.valueOf(obj.get("rewardType").asString)

            return when(rewardType) {
                RewardType.CURRENCY -> SlotCurrencyContent.fromJson(obj)
                RewardType.CARD -> SlotCardContent.fromJson(obj)
            }
        }
    }

    var emoji: CustomEmoji? = null
        private set

    fun load() {
        if (emojiName.isBlank() || emojiID == 0L)
            return

        val e = SlotEmojiContainer.loadedEmoji.filter { emote ->
            if (emote.name != emojiName)
                return@filter false

            return@filter when(emote.type) {
                Emoji.Type.UNICODE -> false
                Emoji.Type.CUSTOM -> emote.idLong == emojiID
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

        obj.addProperty("rewardType", rewardType.name)

        appendJson(obj)

        return obj
    }

    fun changeEmoji(e: CustomEmoji?) {
        emoji = e

        emojiName = e?.name ?: ""
        emojiID = e?.idLong ?: 0L
    }

    abstract fun appendJson(obj: JsonObject)
}