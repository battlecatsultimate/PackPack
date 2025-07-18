package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card

class ECCValidation {
    enum class ValidationWay {
        SEASONAL_15_COLLAB_12_T4,
        T4_2,
        SAME_T4_3,
        LEGENDARY_COLLECTOR,
        NONE
    }

    companion object {
        fun checkDoable(validationWay: ValidationWay, inventory: Inventory) : String {
            val builder = StringBuilder("")

            when(validationWay) {
                ValidationWay.LEGENDARY_COLLECTOR -> {
                    if (!inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                        builder.append("- You aren't currently owning <@&${CardData.Role.LEGEND.id}>!")
                    }
                }
                ValidationWay.SEASONAL_15_COLLAB_12_T4 -> {
                    val seasonalSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.SEASONAL }.union(inventory.ccValidation.cardList.filter { card -> card.cardType == Card.CardType.SEASONAL }).toSet().size
                    val collaborationSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.union(inventory.ccValidation.cardList.filter { card -> card.cardType == Card.CardType.COLLABORATION }).toSet().size

                    if (seasonalSize < 15) {
                        builder.append("- Not enough unique collaboration cards! You currently have $seasonalSize unique card${if (seasonalSize >= 2) "s" else ""}, but 15 cards are required\n")
                    }

                    if (collaborationSize < 12) {
                        builder.append("- Not enough unique collaboration cards! You currently have $collaborationSize unique card${if (collaborationSize >= 2) "s" else ""}, but 12 cards are required\n")
                    }

                    if (inventory.cards.keys.none { c -> c.tier == CardData.Tier.LEGEND }) {
                        builder.append("- You don't have any T4 cards!")
                    }
                }
                ValidationWay.T4_2 -> {
                    val cardSize = inventory.cards.keys.filter { c -> c.tier == CardData.Tier.LEGEND }.toSet().size

                    if (cardSize < 2) {
                        builder.append("- Not enough unique collaboration cards! You currently have $cardSize unique card, but 2 cards are required\n")
                    }
                }
                ValidationWay.SAME_T4_3 -> {
                    if (inventory.cards.keys.filter { c -> c.tier == CardData.Tier.LEGEND }.none { c -> (inventory.cards[c] ?: 0) >= 3 }) {
                        builder.append("- You don't have any T4 cards with amount of 3 or over!")
                    }
                }
                ValidationWay.NONE -> {}
            }

            return builder.toString()
        }
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

    var validationWay = ValidationWay.NONE
    val cardList = ArrayList<Card>()

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("validationWay", validationWay.name)

        val arr = JsonArray()

        cardList.forEach { c -> arr.add(c.id) }

        obj.add("cardList", arr)

        return obj
    }
}