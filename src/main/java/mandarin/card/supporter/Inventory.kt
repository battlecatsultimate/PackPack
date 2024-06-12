package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.packpack.supporter.StaticStore

class Inventory(private val id: Long) {
    var cards = PositiveMap<Card, Int>()
    var favorites = PositiveMap<Card, Int>()
    var auctionQueued = PositiveMap<Card, Int>()

    var vanityRoles = ArrayList<CardData.Role>()

    var catFoods = 0L
    var platinumShard = 0L

    val actualCatFood: Long
        get() {
            return catFoods - tradePendingCatFoods - auctionPendingCatFoods
        }
    val tradePendingCatFoods: Long
        get() {
            return CardData.sessions.filter { s -> id in s.member }.sumOf { s -> s.suggestion[s.member.indexOf(id)].catFood }.toLong()
        }
    val auctionPendingCatFoods: Long
        get() {
            return CardData.auctionSessions.filter { s -> s.bidData.containsKey(id) }.sumOf { s -> s.bidData[id] ?: 0L }
        }

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

    fun transferInventory(other: Inventory, transferMode: CardData.TransferMode) {
        when (transferMode) {
            CardData.TransferMode.INJECT ->  {
                catFoods += other.catFoods
                platinumShard += other.platinumShard

                other.cards.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                other.auctionQueued.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                other.favorites.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                other.vanityRoles.forEach { role ->
                    if (role !in vanityRoles)
                        vanityRoles.add(role)
                }
            }
            CardData.TransferMode.OVERRIDE -> {
                catFoods = other.catFoods
                platinumShard = other.platinumShard

                cards.clear()
                auctionQueued.clear()
                favorites.clear()

                vanityRoles.clear()

                other.cards.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                other.auctionQueued.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                other.favorites.entries.forEach { (card, amount) ->
                    cards[card] = (cards[card] ?: 0) + amount
                }

                vanityRoles.addAll(other.vanityRoles)
            }
        }

        CardBot.saveCardData()
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

        val a = JsonObject()

        for (card in auctionQueued.keys) {
            val number = auctionQueued[card] ?: 1

            a.addProperty(card.unitID.toString(), number)
        }

        obj.add("auctionQueued", a)

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

        for (i in CardData.Tier.COMMON.ordinal..CardData.Tier.UNCOMMON.ordinal) {
            if (CardData.permanents[i].map { index -> CardData.bannerData[i][index] }.any { idSet -> idSet.any { id -> id !in cardsTotal } })
                return false
        }

        val uberFest = cardsTotal.any { id -> id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][0] }
        val epicFest = cardsTotal.any { id -> id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][1] }
        val busters = cardsTotal.any { id -> id == 435 || id == 484 || id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][2] }
        val legends = cards.keys.any { card -> card.tier == CardData.Tier.LEGEND } || favorites.keys.any { card -> card.tier == CardData.Tier.LEGEND }

        return uberFest && epicFest && busters && legends
    }

    fun getInvalidReason() : String {
        val builder = StringBuilder("Missing Condition : \n\n")

        val missingCards = ArrayList<Card>()

        val cardsTotal = this.cards.keys.map { card -> card.unitID }.union(favorites.keys.map { card -> card.unitID })

        for (i in CardData.Tier.COMMON.ordinal..CardData.Tier.UNCOMMON.ordinal) {
            CardData.permanents[i].map { index -> CardData.bannerData[i][index] }.forEach { idSet ->
                idSet.forEach { id ->
                    if (id !in cardsTotal) {
                        val c = CardData.cards.find { c -> c.unitID == id }

                        if (c != null)
                            missingCards.add(c)
                    }
                }
            }
        }

        val uberFest = cardsTotal.any { id -> id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][0] }
        val epicFest = cardsTotal.any { id -> id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][1] }
        val busters = cardsTotal.any { id -> id == 435 || id == 484 || id in CardData.bannerData[CardData.Tier.ULTRA.ordinal][2] }
        val legends = this.cards.keys.any { card -> card.tier == CardData.Tier.LEGEND } || favorites.keys.any { card -> card.tier == CardData.Tier.LEGEND }

        if (missingCards.isNotEmpty()) {
            val cardBuilder = StringBuilder("- Missing T1/T2 cards -> ")

            missingCards.forEachIndexed { index, card ->
                if (cardBuilder.length + card.simpleCardInfo().length > 1024) {
                    cardBuilder.append("... and ${missingCards.size - index - 1} card(s) more")

                    return@forEachIndexed
                }

                cardBuilder.append(card.simpleCardInfo())

                if (index < missingCards.size - 1) {
                    cardBuilder.append(", ")
                }
            }

            builder.append(cardBuilder).append("\n")
        }

        if (!uberFest) {
            builder.append("- Missing uber fest card\n")
        }

        if (!epicFest) {
            builder.append("- Missing epic fest card\n")
        }

        if (!busters) {
            builder.append("- Missing busters card\n")
        }

        if (!legends) {
            builder.append("- Missing T4 [Legend Rare] card")
        }

        return builder.toString().trim()
    }

    companion object {
        fun readInventory(id: Long, obj: JsonObject): Inventory {
            val inventory = Inventory(id)

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

            if (obj.has("auctionQueued")) {
                val cardIDs = obj.getAsJsonObject("auctionQueued")

                for (unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.unitID == unitID.toInt() } ?: continue

                    inventory.auctionQueued[foundCard] = cardIDs.get(unitID).asInt
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

        fun getInventory(id: Long) : Inventory {
            var inventory = CardData.inventories[id]

            if (inventory == null) {
                inventory = Inventory(id)

                CardData.inventories[id] = inventory
            }

            return inventory
        }
    }
}
