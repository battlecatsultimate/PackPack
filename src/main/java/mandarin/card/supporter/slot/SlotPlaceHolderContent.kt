package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

class SlotPlaceHolderContent(emojiName: String, emojiID: Long) : SlotContent(emojiName, emojiID, RewardType.PLACE_HOLDER) {
    companion object {
        fun fromJson(obj: JsonObject) : SlotPlaceHolderContent {
            if (!StaticStore.hasAllTag(obj,"emojiName", "emojiID")) {
                throw IllegalStateException("E/SlotCardContent::fromJson - Invalid data found")
            }

            val content = SlotPlaceHolderContent(obj.get("emojiName").asString, obj.get("emojiID").asLong)

            return content
        }
    }

    override fun appendJson(obj: JsonObject) {

    }
}