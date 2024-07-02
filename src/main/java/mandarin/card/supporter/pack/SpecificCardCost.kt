package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.stream.MalformedJsonException
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import java.util.HashSet

class SpecificCardCost(val cards: HashSet<Card>, amount: Long) : CardCost(CostType.CARD, amount) {
    companion object {
        fun readJson(obj: JsonObject) : SpecificCardCost {
            if (!obj.has("cards")) {
                throw MalformedJsonException("Invalid SpecificCardCost json format")
            }

            val amount = obj.get("amount").asLong
            val cards = HashSet<Card>()

            obj.getAsJsonArray("cards").forEach { e ->
                val cardID = e.asInt
                val card = CardData.cards.find { c -> c.unitID == cardID } ?: return@forEach

                cards.add(card)
            }

            return SpecificCardCost(cards, amount)
        }
    }

    override fun filter(c: Card): Boolean {
        return cards.any { card -> c.unitID == card.unitID }
    }

    override fun finishJson(obj: JsonObject) {
        val arr = JsonArray()

        cards.forEach { c ->
            arr.add(c.unitID)
        }

        obj.add("cards", arr)
    }

    override fun getCostName(): String {
        return cards.map { c -> c.simpleCardInfo() }.joinToString(", ", "{ ", " }") + " x" + amount
    }

    fun simpleCostName() : String {
        return if (cards.isEmpty()) {
            return "{ } x$amount"
        } else if (cards.size == 1) {
            return "{ ${cards.toList().first().simpleCardInfo()} } x$amount"
        } else {
            return "{ ${cards.toList().first().simpleCardInfo()}, ... } x$amount"
        }
    }
}