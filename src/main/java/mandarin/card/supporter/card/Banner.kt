package mandarin.card.supporter.card

import com.google.gson.JsonObject
import mandarin.card.supporter.CardData

class Banner(var name: String, var category: Boolean) {
    companion object {
        val NONE = Banner("None", false)
        val SEASONAL = Banner("Seasonal", false)
        val COLLABORATION = Banner("Collaboration", false)

        fun fromJson(obj: JsonObject) : Banner {
            if (!obj.has("name") || !obj.has("category")) {
                throw IllegalStateException("E/Banner::fromJson - Invalid json data has passed")
            }

            val name = obj.get("name").asString
            val category = obj.get("category").asBoolean

            val banner = Banner(name, category)

            if (obj.has("legendCollector")) {
                banner.legendCollector = obj.get("legendCollector").asBoolean
            }

            return banner
        }

        fun fromName(name: String) : Banner {
            return CardData.banners.find { b -> b.name == name } ?: NONE
        }
    }

    var legendCollector = false

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("name", name)
        obj.addProperty("category", category)

        return obj
    }

    fun collectCards() : List<Card> {
        return if (this === NONE) {
            ArrayList(CardData.cards)
        } else if (this === SEASONAL) {
            CardData.cards.filter { c -> c.cardType == Card.CardType.SEASONAL }
        } else if (this === COLLABORATION) {
            CardData.cards.filter { c -> c.cardType == Card.CardType.COLLABORATION }
        } else {
            CardData.cards.filter { c -> this in c.banner }
        }
    }

    override fun toString(): String {
        return "Banner [$name <$category>]"
    }
}