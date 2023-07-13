package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

class Inventory {
    var cards = HashMap<Card, Int>()
    var vanityRoles = ArrayList<CardData.Role>()

    fun addCards(c: List<Card>) {
        for (card in c) {
            if (cards.containsKey(card)) {
                val numberOfCards = cards[card] ?: 0

                cards[card] = numberOfCards + 1
            } else {
                cards[card] = 1
            }
        }
    }

    fun removeCards(c: List<Card>) {
        for (card in c) {
            if (cards.containsKey(card)) {
                val numberOfCards = cards[card]

                if (numberOfCards == null) {
                    StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card that doesn't exist")

                    continue
                }

                cards[card] = numberOfCards - 1
            } else {
                StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card that doesn't exist")
            }
        }

        cards.entries.removeIf { e -> e.value <= 0 }
    }

    fun toJson(): JsonObject {
        val obj = JsonObject()

        val c = JsonObject()

        for(card in cards.keys) {
            val number = cards[card] ?: 1

            c.addProperty(card.unitID.toString(), number)
        }

        obj.add("cards", c)

        val roleArray = JsonArray()

        for (r in vanityRoles) {
            roleArray.add(r.name)
        }

        obj.add("roles", roleArray)

        return obj
    }

    companion object {
        fun readInventory(obj: JsonObject): Inventory {
            val inventory = Inventory()

            if (obj.has("cards")) {
                val cardIDs = obj.getAsJsonObject("cards")

                for(unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.unitID == unitID.toInt() } ?: continue

                    inventory.cards[foundCard] = cardIDs.get(unitID).asInt
                }
            }

            if (obj.has("roles")) {
                val roleArray = obj.getAsJsonArray("roles")

                for (r in roleArray) {
                    inventory.vanityRoles.add(CardData.Role.valueOf(r.asString))
                }
            }

            return inventory
        }

        fun getInventory(id: String) : Inventory {
            var inventory = CardData.inventories[id]

            if (inventory == null) {
                inventory = Inventory()

                CardData.inventories[id] = inventory
            }

            return inventory
        }
    }
}
