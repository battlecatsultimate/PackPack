package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore

class CardPack {
    companion object {
        fun fromJson(obj: JsonObject) : CardPack {
            if (!obj.has("uuid") || !obj.has("packName") || !obj.has("cost") || !obj.has("cardChancePairLists") || !obj.has("cooldown")) {
                throw IllegalStateException("E/CardPack::fromJson - Invalid json data : $obj")
            }

            val packName = obj.get("packName").asString
            val uuid = obj.get("uuid").asString
            val cost = PackCost.fromJson(obj.getAsJsonObject("cost"))
            val cardChancePairLists = ArrayList<CardChancePairList>()

            obj.getAsJsonArray("cardChancePairLists").forEach { e ->
                cardChancePairLists.add(CardChancePairList.fromJson(e.asJsonObject))
            }

            val cooldown = obj.get("cooldown").asLong

            val pack = CardPack(packName, uuid, cost, cardChancePairLists, cooldown)

            if (obj.has("activated")) {
                pack.activated = obj.get("activated").asBoolean
            }

            return pack
        }
    }

    enum class CardType {
        T1,
        T2,
        REGULAR,
        SEASONAL,
        COLLABORATION,
        T3,
        T4
    }

    constructor(packName: String, cost: PackCost, cardChancePairLists: ArrayList<CardChancePairList>, cooldown: Long) {
        this.packName = packName
        uuid = CardData.getUnixEpochTime().toString() + "|" + packName
        this.cost = cost
        this.cardChancePairLists = cardChancePairLists
        this.cooldown = cooldown
    }

    private constructor(packName: String, uuid: String, cost: PackCost, cardChancePairLists: ArrayList<CardChancePairList>, cooldown: Long) {
        this.packName = packName
        this.uuid = uuid
        this.cost = cost
        this.cardChancePairLists = cardChancePairLists
        this.cooldown = cooldown
    }

    val cost: PackCost
    val cardChancePairLists: ArrayList<CardChancePairList>

    val uuid: String

    var packName: String
    var activated = false
    var cooldown: Long // in milliseconds

    fun displayInfo() : String {
        val builder = StringBuilder("## $packName\n### Pack Cost\n")

        if (cost.catFoods > 0) {
            builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} ${cost.catFoods}\n")
        }

        if (cost.platinumShards > 0) {
            builder.append("- ${EmojiStore.ABILITY["SHARD"]?.formatted} ${cost.platinumShards}\n")
        }

        if (cost.roles.isNotEmpty()) {
            if (cost.roles.size == 1) {
                builder.append("\nYou also have to have <@&").append(cost.roles[0]).append("> role")
            } else {
                builder.append("\nYou also have to have either one of these roles below :\n\n")

                cost.roles.forEachIndexed { index, id ->
                    builder.append("<@&").append(id).append(">")

                    if (index < cost.roles.size - 1)
                        builder.append(", ")
                }
            }

            builder.append("\n")
        }

        for (cardCost in cost.cardsCosts) {
            builder.append("- ").append(cardCost.getCostName()).append("\n")
        }

        builder.append("### Contents\n")

        for (group in cardChancePairLists) {
            builder.append(group.getInfo()).append("\n")
        }

        builder.append("### Cooldown\n").append(CardData.convertMillisecondsToText(cooldown))

        if (
            cost.cardsCosts.any { cardCost -> cardCost is TierCardCost && CardType.T2 == cardCost.tier } &&
            cost.cardsCosts.any { cardCost -> cardCost is TierCardCost && CardType.REGULAR == cardCost.tier }
        ) {
            builder.append("\n\nDifference between normal tier 2 card and regular tier 2 card is that normal tier 2 card includes all type of tier 2 cards such as seasonal units or collaboration units\n" +
                    "On the other hand, regular tier 2 cards doesn't allow seasonal and collaboration")
        }

        return builder.toString()
    }

    fun isInvalid() : Boolean {
        return cardChancePairLists.isEmpty() || cardChancePairLists.any { cardChancePairList -> cardChancePairList.pairs.isEmpty() || cardChancePairList.pairs.any { pair -> pair.cardGroup.extra.isEmpty() && pair.cardGroup.types.isEmpty() } }
    }

    fun roll() : List<Card> {
        val result = ArrayList<Card>()

        for (cardChancePairList in cardChancePairLists) {
            result.addAll(cardChancePairList.roll())
        }

        return result
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("packName", packName)
        obj.addProperty("uuid", uuid)
        obj.add("cost", cost.asJson())
        obj.addProperty("activated", activated)

        val cardChancePairArr = JsonArray()

        cardChancePairLists.forEach { cardChancePairList ->
            cardChancePairArr.add(cardChancePairList.asJson())
        }

        obj.add("cardChancePairLists", cardChancePairArr)
        obj.addProperty("cooldown", cooldown)

        return obj
    }
}