package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

class Inventory {
    var cards = HashMap<Card, Int>()
    var favorites = HashMap<Card, Int>()

    var vanityRoles = ArrayList<CardData.Role>()

    var catFoods = 0L
    var platinumShard = 0L

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
            if (cards.containsKey(card) || favorites.containsKey(card)) {
                val numberOfCards = cards[card] ?: 0
                val numberOfFavorites = favorites[card] ?: 0

                if (numberOfCards + numberOfFavorites == 0) {
                    StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card that doesn't exist")

                    continue
                }

                if (numberOfCards != 0)
                    cards[card] = numberOfCards - 1
                else if (numberOfFavorites != 0)
                    favorites[card] = numberOfFavorites - 1
            } else {
                StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card that doesn't exist")
            }
        }

        cards.entries.removeIf { e -> e.value <= 0 }
        favorites.entries.removeIf { e -> e.value <= 0 }
    }

    fun favoriteCards(c: Card, amount: Int) {
        val cardAmount = cards[c] ?: 0

        if (cardAmount < amount) {
            StaticStore.logger.uploadLog("W/Inventory::favoriteCards - Tried to favorite card that doesn't have enough amount $amount, currently having $cardAmount")

            return
        }

        cards[c] = (cards[c] ?: 0) - amount
        favorites[c] = (favorites[c] ?: 0) + amount

        cards.entries.removeIf { e -> e.value <= 0 }
        favorites.entries.removeIf { e -> e.value <= 0 }
    }

    fun unfavoriteCards(c: Card, amount: Int) {
        val cardAmount = favorites[c] ?: 0

        if (cardAmount < amount) {
            StaticStore.logger.uploadLog("W/Inventory::unfavoriteCards - Tried to unfavorite card that doesn't have enough amount $amount, currently having $cardAmount")

            return
        }

        cards[c] = (cards[c] ?: 0) + amount
        favorites[c] = (favorites[c] ?: 0) - amount

        cards.entries.removeIf { e -> e.value <= 0 }
        favorites.entries.removeIf { e -> e.value <= 0 }
    }

    fun toJson(): JsonObject {
        val obj = JsonObject()

        val c = JsonObject()

        for(card in cards.keys) {
            val number = cards[card] ?: 1

            c.addProperty(card.unitID.toString(), number)
        }

        obj.add("cards", c)

        val f = JsonObject()

        for (card in favorites.keys) {
            val number = favorites[card] ?: 1

            f.addProperty(card.unitID.toString(), number)
        }

        obj.add("favorites", f)

        val roleArray = JsonArray()

        for (r in vanityRoles) {
            roleArray.add(r.name)
        }

        obj.add("roles", roleArray)

        obj.addProperty("catFoods", catFoods)
        obj.addProperty("platinumShard", platinumShard)

        return obj
    }

    fun validForLegendCollector() : Boolean {
        val cardsTotal = cards.keys.map { card -> card.unitID }.union(favorites.keys.map { card -> card.unitID })

        for (i in 0..1) {
            if (CardData.permanents[i].map { index -> CardData.bannerData[i][index] }.any { idSet -> idSet.any { id -> id !in cardsTotal } })
                return false
        }

        val uberFest = cardsTotal.any { id -> id in CardData.bannerData[2][0] }
        val epicFest = cardsTotal.any { id -> id in CardData.bannerData[2][1] }
        val busters = cardsTotal.any { id -> id == 435 || id == 484 || id in CardData.bannerData[2][2] }
        val legends = cards.keys.any { card -> card.tier == CardData.Tier.LEGEND } || favorites.keys.any { card -> card.tier == CardData.Tier.LEGEND }

        return uberFest && epicFest && busters && legends
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

            if (obj.has("favorites")) {
                val cardIDs = obj.getAsJsonObject("favorites")

                for (unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.unitID == unitID.toInt() } ?: continue

                    inventory.favorites[foundCard] = cardIDs.get(unitID).asInt
                }
            }

            if (obj.has("roles")) {
                val roleArray = obj.getAsJsonArray("roles")

                for (r in roleArray) {
                    when (r.asString) {
                        "ASSASSIN" -> inventory.vanityRoles.add(CardData.Role.BAKOO)
                        "ANGELIC" -> inventory.vanityRoles.add(CardData.Role.YOUCAN)
                        "SIRSEAL" -> inventory.vanityRoles.add(CardData.Role.AHIRUJO)
                        "MOOTH" -> inventory.vanityRoles.add(CardData.Role.GOBBLE)
                        else -> inventory.vanityRoles.add(CardData.Role.valueOf(r.asString))
                    }
                }
            }

            if (obj.has("catFoods")) {
                inventory.catFoods = obj.get("catFoods").asLong
            }

            if (obj.has("platinumShard")) {
                inventory.platinumShard = obj.get("platinumShard").asLong
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
