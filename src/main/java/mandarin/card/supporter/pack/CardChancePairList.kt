package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import kotlin.random.Random

class CardChancePairList(var amount: Int) {
    companion object {
        fun fromJson(obj: JsonObject) : CardChancePairList {
            val cardChancePairList = CardChancePairList(0)

            if (obj.has("amount")) {
                cardChancePairList.amount = obj.get("amount").asInt
            }

            if (obj.has("pairs")) {
                obj.getAsJsonArray("pairs").forEach { e ->
                    cardChancePairList.pairs.add(CardChancePair.fromJson(e.asJsonObject))
                }
            }

            return cardChancePairList
        }
    }

    val pairs = ArrayList<CardChancePair>()

    fun validateChance() {
        val total = pairs.sumOf { g -> g.chance }

        if (total != 100.0) {
            pairs.forEach { g -> g.chance = g.chance / total * 100 }
        }
    }

    fun roll() : List<Card> {
        val result = ArrayList<Card>()

        repeat(amount) {
            result.add(pickCard())
        }

        return result
    }

    private fun pickCard() : Card {
        val chance = Random.nextDouble() * 100.0
        var random = 0.0

        for (pair in pairs) {
            random += pair.chance

            if (chance <= random)
                return pair.cardGroup.gatherPools().random()
        }

        return pairs.last().cardGroup.gatherPools().random()
    }

    fun getInfo() : String {
        val builder = StringBuilder()

        builder.append("- ").append(amount).append(" card")

        if (amount > 1) {
            builder.append("s")
        }

        builder.append("\n")

        for (i in pairs.indices) {
            builder.append("  - ").append(CardData.df.format(pairs[i].chance)).append("% : ").append(pairs[i].cardGroup.getName())

            if (i < pairs.size - 1)
                builder.append("\n")
        }

        return builder.toString()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("amount", amount)

        val pairArr = JsonArray()

        pairs.forEach { pair ->
            pairArr.add(pair.asJson())
        }

        obj.add("pairs", pairArr)

        return obj
    }
}