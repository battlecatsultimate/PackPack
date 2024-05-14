package mandarin.card.supporter.slot

import com.google.gson.JsonObject
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore

class SlotPrice {
    companion object {
        fun fromJson(obj: JsonObject) : SlotPrice {
            if (!StaticStore.hasAllTag(obj, "catFoodAllowed", "minimumCatFoods", "maximumCatFoods", "platinumShardAllowed", "minimumPlatinumShards", "maximumPlatinumShards")) {
                throw IllegalStateException("E/SlotPrice::fromJson - Invalid json data found")
            }

            val price = SlotPrice()

            price.catFoodAllowed = obj.get("catFoodAllowed").asBoolean

            price.minimumCatFoods = obj.get("minimumCatFoods").asLong
            price.maximumCatFoods = obj.get("maximumCatFoods").asLong

            price.platinumShardAllowed = obj.get("platinumShardAllowed").asBoolean

            price.minimumPlatinumShards = obj.get("minimumPlatinumShards").asLong
            price.maximumPlatinumShards = obj.get("maximumPlatinumShards").asLong

            return price
        }
    }

    var catFoodAllowed = true

    var minimumCatFoods = 0L
    var maximumCatFoods = 100000L

    var platinumShardAllowed = false

    var minimumPlatinumShards = 0L
    var maximumPlatinumShards = 100L

    fun asText() : String {
        val cf = EmojiStore.ABILITY["CF"]?.formatted
        val ps = EmojiStore.ABILITY["PS"]?.formatted

        var result = "Is Cat Foods $cf Allowed? : "

        result += if (catFoodAllowed) {
            "Yes\n\nMinimum Cat Foods : $cf $minimumCatFoods\nMaximum Cat Foods : $cf $maximumCatFoods"
        } else {
            "No"
        }

        result += "\n\nIs Platinum Cards $ps Allowed? : "

        result += if (platinumShardAllowed) {
            "Yes\n\nMinimum Platinum Shards : $ps $minimumPlatinumShards\nMaximum Platinum Shards : $ps $maximumPlatinumShards"
        } else {
            "No"
        }

        return result
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("catFoodAllowed", catFoodAllowed)

        obj.addProperty("minimumCatFoods", minimumCatFoods)
        obj.addProperty("maximumCatFoods", maximumCatFoods)

        obj.addProperty("platinumShardAllowed", platinumShardAllowed)

        obj.addProperty("minimumPlatinumShards", minimumPlatinumShards)
        obj.addProperty("maximumPlatinumShards", maximumPlatinumShards)

        return obj
    }
}