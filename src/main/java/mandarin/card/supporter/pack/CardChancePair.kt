package mandarin.card.supporter.pack

import com.google.gson.JsonObject

class CardChancePair(
    var chance: Double,
    val cardGroup: CardGroupData
) {
    companion object {
        fun fromJson(obj: JsonObject) : CardChancePair {
            if (!obj.has("cardGroup") || !obj.has("chance")) {
                throw IllegalStateException("E/CardChancePair::fromJson - Invalid json data : $obj")
            }

            val cardGroupData = CardGroupData.fromJson(obj.getAsJsonObject("cardGroup"))
            val chance = obj.get("chance").asDouble

            return CardChancePair(chance, cardGroupData)
        }
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("chance", chance)
        obj.add("cardGroup", cardGroup.asJson())

        return obj
    }
}