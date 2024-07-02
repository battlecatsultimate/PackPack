package mandarin.card.supporter.pack

import com.google.gson.JsonObject
import com.google.gson.stream.MalformedJsonException
import mandarin.card.supporter.Card

abstract class CardCost(private val type: CostType, var amount: Long) {
    companion object {
        fun fromJson(obj: JsonObject) : CardCost {
            if (obj.has("type") && obj.has("amount")) {
                val type = CostType.valueOf(obj.get("type").asString)

                return when(type) {
                    CostType.BANNER -> {
                        BannerCardCost.readJson(obj)
                    }
                    CostType.TIER -> {
                        TierCardCost.readJson(obj)
                    }
                    CostType.CARD -> {
                        SpecificCardCost.readJson(obj)
                    }
                }
            }

            throw MalformedJsonException("Corrupted card cost")
        }
    }

    enum class CostType {
        BANNER,
        TIER,
        CARD
    }

    abstract fun filter(c: Card) : Boolean

    abstract fun finishJson(obj: JsonObject)

    abstract fun getCostName() : String

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("type", type.name)
        obj.addProperty("amount", amount)

        finishJson(obj)

        return obj
    }
}