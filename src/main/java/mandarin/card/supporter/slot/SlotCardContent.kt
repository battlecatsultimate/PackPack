package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardGroupData
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.StaticStore

class SlotCardContent(emojiName: String, emojiID: Long) : SlotContent(emojiName, emojiID, RewardType.CARD) {
    companion object {
        fun fromJson(obj: JsonObject) : SlotCardContent {
            if (!StaticStore.hasAllTag(obj,"emojiName", "emojiID", "cardChancePairLists", "name")) {
                throw IllegalStateException("E/SlotCardContent::fromJson - Invalid data found")
            }

            val content = SlotCardContent(obj.get("emojiName").asString, obj.get("emojiID").asLong)

            content.slot = obj.get("slot").asInt
            content.name = obj.get("name").asString

            obj.getAsJsonArray("cardChancePairLists").forEach { e ->
                if (e !is JsonObject)
                    return@forEach

                content.cardChancePairLists.add(CardChancePairList.fromJson(e))
            }

            return content
        }
    }

    val cardChancePairLists = ArrayList<CardChancePairList>()

    var name = ""

    override fun appendJson(obj: JsonObject) {
        obj.addProperty("name", name)

        val arr = JsonArray()

        cardChancePairLists.forEach { c ->
            arr.add(c.asJson())
        }

        obj.add("cardChancePairLists", arr)
    }

    fun roll() : List<Card> {
        val result = ArrayList<Card>()

        for (cardChancePairList in cardChancePairLists) {
            result.addAll(cardChancePairList.roll())
        }

        return result
    }

    fun injectCardPack(pack: CardPack) {
        cardChancePairLists.clear()

        pack.cardChancePairLists.forEach { cardChancePairList ->
            val cloned = CardChancePairList(cardChancePairList.amount)

            cardChancePairList.pairs.forEach { pair ->
                val clonePair = CardChancePair(pair.chance, CardGroupData(ArrayList(), ArrayList()))

                clonePair.cardGroup.types.addAll(pair.cardGroup.types)
                clonePair.cardGroup.extra.addAll(pair.cardGroup.extra)

                cloned.pairs.add(pair)
            }

            cardChancePairLists.add(cloned)
        }

        if (name.isBlank()) {
            name = pack.packName
        }
    }
}