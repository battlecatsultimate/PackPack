package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card

class ECCValidation {
    enum class ValidationWay {
        LEGENDARY_COLLECTOR,
        SEASONAL_15_COLLAB_12_T4,
        T4_2,
        SAME_T4_3,
        NONE
    }

    companion object {
        fun fromJson(obj: JsonObject) : ECCValidation {
            val validation = ECCValidation()

            if (obj.has("validationWay")) {
                validation.validationWay = ValidationWay.valueOf(obj.get("validationWay").asString)
            }

            if (obj.has("cardList")) {
                obj.getAsJsonArray("cardList").forEach { e ->
                    val c = CardData.cards.find { c -> c.id == e.asInt } ?: return@forEach

                    validation.cardList.add(c)
                }
            }

            return validation
        }
    }

    private var validationWay = ValidationWay.NONE
    private val cardList = ArrayList<Card>()

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("validationWay", validationWay.name)

        val arr = JsonArray()

        cardList.forEach { c -> arr.add(c.id) }

        obj.add("cardList", arr)

        return obj
    }
}