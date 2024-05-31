package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore

class SlotEntryFee {
    enum class EntryType {
        CAT_FOOD,
        PLATINUM_SHARDS
    }

    companion object {
        fun fromJson(obj: JsonObject) : SlotEntryFee {
            if (!StaticStore.hasAllTag(obj, "entryType", "minimumFee", "maximumFee")) {
                throw IllegalStateException("E/SlotPrice::fromJson - Invalid json data found")
            }

            val price = SlotEntryFee()

            price.entryType = EntryType.valueOf(obj.get("entryType").asString)

            price.minimumFee = obj.get("minimumFee").asLong
            price.maximumFee = obj.get("maximumFee").asLong

            return price
        }
    }

    var entryType = EntryType.CAT_FOOD

    var minimumFee = 0L
    var maximumFee = 100000L

    val invalid: Boolean
        get() {
            return minimumFee > maximumFee || maximumFee == 0L
        }

    fun asText() : String {
        val emoji = when(entryType) {
            EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        val priceName = when(entryType) {
            EntryType.CAT_FOOD -> "Cat Foods $emoji"
            EntryType.PLATINUM_SHARDS -> "Platinum Shards $emoji"
        }

        return "Entry Type : $priceName\n" +
                "\n" +
                "Minimum Fee : $emoji $minimumFee\n" +
                "Maximum Fee : $emoji $maximumFee"
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("entryType", entryType.name)

        obj.addProperty("minimumFee", minimumFee)
        obj.addProperty("maximumFee", maximumFee)

        return obj
    }
}