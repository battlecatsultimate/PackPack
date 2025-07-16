package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore

class CCValidation {
    enum class ValidationWay {
        SEASONAL_15,
        COLLABORATION_12,
        SEASONAL_15_COLLABORATION_12,
        T3_3,
        LEGENDARY_COLLECTOR,
        NONE
    }

    companion object {
        fun checkDoable(validationWay: ValidationWay, inventory: Inventory) : String {
            val builder = StringBuilder("")

            when(validationWay) {
                ValidationWay.SEASONAL_15 -> {
                    val cardSize = inventory.cards.keys.filter { card ->  card.cardType == Card.CardType.SEASONAL }.toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 15) {
                        builder.append("- Not enough unique seasonal cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 15 cards are required\n")
                    }

                    if (inventory.actualCatFood < 150000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 150000 is required")
                    }
                }
                ValidationWay.COLLABORATION_12 -> {
                    val cardSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 12) {
                        builder.append("- Not enough unique collaboration cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 12 cards are required\n")
                    }

                    if (inventory.actualCatFood < 150000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 150000 is required")
                    }
                }
                ValidationWay.SEASONAL_15_COLLABORATION_12 -> {
                    val seasonalSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.SEASONAL }.toSet().size
                    val collaborationSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.toSet().size

                    if (seasonalSize < 15) {
                        builder.append("- Not enough unique collaboration cards! You currently have $seasonalSize unique card${if (seasonalSize >= 2) "s" else ""}, but 15 cards are required\n")
                    }

                    if (collaborationSize < 12) {
                        builder.append("- Not enough unique collaboration cards! You currently have $collaborationSize unique card${if (collaborationSize >= 2) "s" else ""}, but 12 cards are required\n")
                    }
                }
                ValidationWay.T3_3 -> {
                    val cardSize = inventory.cards.keys.filter { card -> card.tier == CardData.Tier.ULTRA }.toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 3) {
                        builder.append("- Not enough unique T3 cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 3 cards are required\n")
                    }

                    if (inventory.actualCatFood < 200000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 200000 is required")
                    }
                }
                ValidationWay.LEGENDARY_COLLECTOR -> {
                    if (!inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                        builder.append("- You aren't currently owning <@&${CardData.Role.LEGEND.id}>!")
                    }
                }
                ValidationWay.NONE -> {

                }
            }

            return builder.toString()
        }

        fun fromJson(obj: JsonObject) : CCValidation {
            val validation = CCValidation()

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