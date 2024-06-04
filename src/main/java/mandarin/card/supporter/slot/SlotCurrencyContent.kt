package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

class SlotCurrencyContent(emojiName: String, emojiID: Long) : SlotContent(emojiName, emojiID, RewardType.CURRENCY) {
    enum class Mode {
        FLAT,
        PERCENTAGE
    }

    companion object {
        fun fromJson(obj: JsonObject) : SlotCurrencyContent {
            if (!StaticStore.hasAllTag(obj,"emojiName", "emojiID", "mode", "amount")) {
                throw IllegalStateException("E/SlotCurrencyContent::fromJson - Invalid data found")
            }

            val content = SlotCurrencyContent(obj.get("emojiName").asString, obj.get("emojiID").asLong)

            content.slot = obj.get("slot").asInt
            content.mode = Mode.valueOf(obj.get("mode").asString)
            content.amount = obj.get("amount").asLong

            return content
        }
    }

    var mode = Mode.PERCENTAGE
    var amount = 0L

    override fun appendJson(obj: JsonObject) {
        obj.addProperty("mode", mode.name)
        obj.addProperty("amount", amount)
    }
}