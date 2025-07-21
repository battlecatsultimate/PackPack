package mandarin.card.supporter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import java.io.File
import java.io.FileWriter
import java.util.HashMap
import java.util.HashSet
import kotlin.math.min

class Inventory(private val id: Long) {
    enum class ShareStatus {
        CC,
        ECC,
        BOTH
    }

    enum class CCValidationWay {
        SEASONAL_15,
        COLLABORATION_12,
        SEASONAL_15_COLLABORATION_12,
        T3_3,
        LEGENDARY_COLLECTOR,
        NONE
    }

    enum class ECCValidationWay {
        SEASONAL_15_COLLAB_12_T4,
        T4_2,
        SAME_T4_3,
        LEGENDARY_COLLECTOR,
        NONE
    }

    var cards = PositiveMap<Card, Int>()
    var favorites = PositiveMap<Card, Int>()
    var auctionQueued = PositiveMap<Card, Int>()

    var ccValidationWay = CCValidationWay.NONE
    var eccValidationWay = ECCValidationWay.NONE
    var validationCards = HashMap<Card, Pair<ShareStatus, Int>>()

    var vanityRoles = ArrayList<CardData.Role>()

    var skins = HashSet<Skin>()
    var equippedSkins = HashMap<Card, Skin>()

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
    val actualFakeCatFood: Long
        get() {
            return catFoods - tradePendingCatFoods - auctionPendingCatFoods - (CardData.taxedCatFoods[id] ?: 0L)
        }

    fun addCards(c: Map<Card, Int>) {
        c.forEach { (card, amount) ->
            cards[card] = (cards[card] ?: 0) + amount
        }
    }

    fun addCards(c: List<Card>) {
        c.forEach { card ->
            cards[card] = (cards[card] ?: 0) + 1
        }
    }

    fun removeCards(c: Map<Card, Int>) {
        c.forEach { (card, amount) ->
            var leftAmount = amount

            val numberOfCards = cards[card] ?: 0
            val numberOfFavorites = favorites[card] ?: 0

            if (numberOfCards + numberOfFavorites == 0) {
                StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card that doesn't exist")

                return@forEach
            }

            if (numberOfCards + numberOfFavorites < amount) {
                StaticStore.logger.uploadLog("W/Inventory::removeCards - Tried to remove card more than what user had")

                return@forEach
            }

            val previousAmount = (cards[card] ?: 0)
            cards[card] = (cards[card] ?: 0) - min(cards[card] ?: 0, leftAmount)
            leftAmount -= previousAmount

            if (leftAmount > 0) {
                favorites[card] = (favorites[card] ?: 0) - min(favorites[card] ?: 0, leftAmount)
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

            c.addProperty(card.id.toString(), number)
        }

        obj.add("cards", c)

        val f = JsonObject()

        for (card in favorites.keys) {
            val number = favorites[card] ?: 1

            f.addProperty(card.id.toString(), number)
        }

        obj.add("favorites", f)

        val s = JsonArray()

        for (skin in skins) {
            s.add(skin.skinID)
        }

        obj.add("skins", s)

        val e = JsonArray()

        equippedSkins.entries.forEach { (card, skin) ->
            val o = JsonObject()

            o.addProperty("key", card.id)
            o.addProperty("val", skin.skinID)

            e.add(o)
        }

        obj.add("equippedSkins", e)

        val a = JsonObject()

        for (card in auctionQueued.keys) {
            val number = auctionQueued[card] ?: 1

            a.addProperty(card.id.toString(), number)
        }

        obj.add("auctionQueued", a)

        val roleArray = JsonArray()

        for (r in vanityRoles) {
            roleArray.add(r.name)
        }

        obj.add("roles", roleArray)

        obj.addProperty("catFoods", catFoods)
        obj.addProperty("platinumShard", platinumShard)

        obj.addProperty("ccValidationWay", ccValidationWay.name)
        obj.addProperty("eccValidationWay", eccValidationWay.name)

        val validationArray = JsonArray()

        validationCards.entries.forEach { (card, pair) ->
            val validationObject = JsonObject()

            validationObject.addProperty("key", card.id)
            validationObject.addProperty("val1", pair.first.name)
            validationObject.addProperty("val2", pair.second)

            validationArray.add(validationObject)
        }

        obj.add("validationCards", validationArray)

        return obj
    }

    fun validForLegendCollector() : Boolean {
        val cardsTotal = cards.keys.union(favorites.keys)

        CardData.banners.filter { b -> b.legendCollector }.forEach { b ->
            CardData.cards.filter { c -> b in c.banner }.forEach { c ->
                if (c !in cardsTotal) {
                    return false
                }
            }
        }

        val epicFest = cardsTotal.any { c -> Banner.fromName("Epicfest Exclusives") in c.banner }
        val uberFest = cardsTotal.any { c -> Banner.fromName("Uberfest Exclusives") in c.banner }
        val busters = cardsTotal.any { c -> Banner.fromName("Other Exclusives") in c.banner }
        val legends = cardsTotal.any { c -> c.tier == CardData.Tier.LEGEND }

        return uberFest && epicFest && busters && legends
    }

    fun getInvalidReason() : String {
        val builder = StringBuilder("Missing Condition : \n\n")

        val missingCards = ArrayList<Card>()

        val cardsTotal = this.cards.keys.union(favorites.keys)

        CardData.banners.filter { b -> b.legendCollector }.forEach { b ->
            CardData.cards.filter { c -> b in c.banner }.forEach { c ->
                if (c !in cardsTotal) {
                    missingCards.add(c)
                }
            }
        }

        val epicFest = cardsTotal.any { c -> Banner.fromName("Epicfest Exclusives") in c.banner }
        val uberFest = cardsTotal.any { c -> Banner.fromName("Uberfest Exclusives") in c.banner }
        val busters = cardsTotal.any { c -> Banner.fromName("Other Exclusives") in c.banner }
        val legends = cardsTotal.any { c -> c.tier == CardData.Tier.LEGEND }

        if (missingCards.isNotEmpty()) {
            val cardBuilder = StringBuilder("- Missing T1/T2 cards -> ")

            run appender@ {
                missingCards.forEachIndexed { index, card ->
                    if (cardBuilder.length + card.simpleCardInfo().length > 1024) {
                        cardBuilder.append("... and ${missingCards.size - index - 1} card(s) more")

                        return@appender
                    }

                    cardBuilder.append(card.simpleCardInfo())

                    if (index < missingCards.size - 1) {
                        cardBuilder.append(", ")
                    }
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

    fun cancelCC(g: Guild, m: Member) {
        val entries = ArrayList(validationCards.entries)

        entries.forEach { (card, pair) ->
            cards[card] = (cards[card] ?: 0) + pair.second
        }

        validationCards.clear()

        when (ccValidationWay) {
            CCValidationWay.SEASONAL_15,
            CCValidationWay.COLLABORATION_12 -> catFoods += 150000
            CCValidationWay.SEASONAL_15_COLLABORATION_12 -> {}
            CCValidationWay.T3_3 -> catFoods += 200000
            CCValidationWay.LEGENDARY_COLLECTOR -> {}
            CCValidationWay.NONE -> {}
        }

        val cc = g.roles.find { r -> r.id == CardData.cc } ?: return
        val ecc = g.roles.find { r -> r.id == CardData.ecc } ?: return

        g.removeRoleFromMember(UserSnowflake.fromId(m.idLong), cc).queue()
        g.removeRoleFromMember(UserSnowflake.fromId(m.idLong), ecc).queue()
    }

    fun cancelECC(g: Guild, m: Member) {
        val entries = ArrayList(validationCards.entries)

        entries.forEach { (card, pair) ->
            when(pair.first) {
                ShareStatus.CC -> return@forEach
                ShareStatus.ECC -> {
                    cards[card] = (cards[card] ?: 0) + pair.second
                    validationCards.remove(card)
                }
                ShareStatus.BOTH -> {
                    validationCards[card] = Pair(ShareStatus.CC, pair.second)
                }
            }
        }

        val ecc = g.roles.find { r -> r.id == CardData.ecc } ?: return

        g.removeRoleFromMember(UserSnowflake.fromId(m.idLong), ecc).queue()
    }

    fun extractAsFile() : File {
        val json = toJson()

        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

        val nodes = mapper.readTree(json.asJsonObject.toString())

        val targetFile = StaticStore.generateTempFile(File("./temp"), "inventory", "json", false)

        val writer = FileWriter(targetFile)

        writer.append(mapper.writeValueAsString(nodes))
        writer.close()

        return targetFile
    }

    companion object {
        fun readInventory(id: Long, obj: JsonObject): Inventory {
            val inventory = Inventory(id)

            if (obj.has("cards")) {
                val cardIDs = obj.getAsJsonObject("cards")

                for(unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.id == unitID.toInt() } ?: continue

                    inventory.cards[foundCard] = cardIDs.get(unitID).asInt
                }
            }

            if (obj.has("favorites")) {
                val cardIDs = obj.getAsJsonObject("favorites")

                for (unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.id == unitID.toInt() } ?: continue

                    inventory.favorites[foundCard] = cardIDs.get(unitID).asInt
                }
            }

            if (obj.has("skins")) {
                val skins = obj.getAsJsonArray("skins")

                skins.forEach { e ->
                    val skinID = e.asInt

                    val foundSkin = CardData.skins.find { s -> s.skinID == skinID } ?: return@forEach

                    inventory.skins.add(foundSkin)
                }
            }

            if (obj.has("equippedSkins")) {
                val equippedSkins = obj.getAsJsonArray("equippedSkins")

                equippedSkins.forEach { e ->
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val cardID = o.get("key").asInt

                        val foundCard = CardData.cards.find { c -> c.id == cardID } ?: return@forEach

                        val skinID = o.get("val").asInt

                        val foundSkin = CardData.skins.find { s -> s.skinID == skinID } ?: return@forEach

                        inventory.equippedSkins[foundCard] = foundSkin
                    }
                }
            }

            if (obj.has("auctionQueued")) {
                val cardIDs = obj.getAsJsonObject("auctionQueued")

                for (unitID in cardIDs.keySet()) {
                    val foundCard = CardData.cards.find { c -> c.id == unitID.toInt() } ?: continue

                    inventory.auctionQueued[foundCard] = cardIDs.get(unitID).asInt
                }
            }

            if (obj.has("roles")) {
                val roleArray = obj.getAsJsonArray("roles")

                for (r in roleArray) {
                    when (r.asString) {
                        "ANGELIC" -> inventory.vanityRoles.add(CardData.Role.YOUCAN)
                        "MOOTH" -> inventory.vanityRoles.add(CardData.Role.GOBBLE)
                        else -> try {
                            inventory.vanityRoles.add(CardData.Role.valueOf(r.asString))
                        } catch (_: IllegalArgumentException) {

                        }
                    }
                }
            }

            if (obj.has("catFoods")) {
                inventory.catFoods = obj.get("catFoods").asLong
            }

            if (obj.has("platinumShard")) {
                inventory.platinumShard = obj.get("platinumShard").asLong
            }

            if (obj.has("ccValidationWay")) {
                inventory.ccValidationWay = CCValidationWay.valueOf(obj.get("ccValidationWay").asString)
            }

            if (obj.has("eccValidationWay")) {
                inventory.eccValidationWay = ECCValidationWay.valueOf(obj.get("eccValidationWay").asString)
            }

            if (obj.has("validationCards")) {
                obj.getAsJsonArray("validationCards").filterIsInstance<JsonObject>().forEach { o ->
                    val card = CardData.cards.find { c -> c.id == o.get("key").asInt } ?: return@forEach

                    val status = ShareStatus.valueOf(o.get("val1").asString)
                    val amount = o.get("val2").asInt

                    inventory.validationCards[card] = Pair(status, amount)
                }
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

        fun checkCCDoable(validationWay: CCValidationWay, inventory: Inventory) : String {
            val builder = StringBuilder("")

            when(validationWay) {
                CCValidationWay.SEASONAL_15 -> {
                    val cardSize = inventory.cards.keys.filter { card ->  card.cardType == Card.CardType.SEASONAL }.union(inventory.validationCards.filterKeys { k -> k.cardType == Card.CardType.SEASONAL }.keys).toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 15) {
                        builder.append("- Not enough unique seasonal cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 15 cards are required\n")
                    }

                    if (inventory.actualCatFood < 150000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 150000 is required")
                    }
                }
                CCValidationWay.COLLABORATION_12 -> {
                    val cardSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.union(inventory.validationCards.filterKeys { k -> k.cardType == Card.CardType.COLLABORATION }.keys).toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 12) {
                        builder.append("- Not enough unique collaboration cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 12 cards are required\n")
                    }

                    if (inventory.actualCatFood < 150000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 150000 is required")
                    }
                }
                CCValidationWay.SEASONAL_15_COLLABORATION_12 -> {
                    val seasonalSize = inventory.cards.keys.filter { card ->  card.cardType == Card.CardType.SEASONAL }.union(inventory.validationCards.filterKeys { k -> k.cardType == Card.CardType.SEASONAL }.keys).toSet().size
                    val collaborationSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.union(inventory.validationCards.filterKeys { k -> k.cardType == Card.CardType.COLLABORATION }.keys).toSet().size

                    if (seasonalSize < 15) {
                        builder.append("- Not enough unique collaboration cards! You currently have $seasonalSize unique card${if (seasonalSize >= 2) "s" else ""}, but 15 cards are required\n")
                    }

                    if (collaborationSize < 12) {
                        builder.append("- Not enough unique collaboration cards! You currently have $collaborationSize unique card${if (collaborationSize >= 2) "s" else ""}, but 12 cards are required\n")
                    }
                }
                CCValidationWay.T3_3 -> {
                    val cardSize = inventory.cards.keys.filter { card -> card.tier == CardData.Tier.ULTRA }.union(inventory.validationCards.filterKeys { k -> k.tier == CardData.Tier.ULTRA }.keys).toSet().size
                    val cf = EmojiStore.ABILITY["CF"]?.formatted

                    if (cardSize < 3) {
                        builder.append("- Not enough unique T3 cards! You currently have $cardSize unique card${if (cardSize >= 2) "s" else ""}, but 3 cards are required\n")
                    }

                    if (inventory.actualCatFood < 200000) {
                        builder.append("- Not enough $cf! You currently have $cf ${inventory.actualCatFood}, but $cf 200000 is required")
                    }
                }
                CCValidationWay.LEGENDARY_COLLECTOR -> {
                    if (!inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                        builder.append("- You aren't currently owning <@&${CardData.Role.LEGEND.id}>!")
                    }
                }
                CCValidationWay.NONE -> {

                }
            }

            return builder.toString()
        }

        fun checkECCDoable(validationWay: ECCValidationWay, inventory: Inventory) : String {
            val builder = StringBuilder("")

            when(validationWay) {
                ECCValidationWay.LEGENDARY_COLLECTOR -> {
                    if (!inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                        builder.append("- You aren't currently owning <@&${CardData.Role.LEGEND.id}>!")
                    }
                }
                ECCValidationWay.SEASONAL_15_COLLAB_12_T4 -> {
                    val seasonalSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.SEASONAL }.union(inventory.validationCards.filterKeys { card -> card.cardType == Card.CardType.SEASONAL }.keys).toSet().size
                    val collaborationSize = inventory.cards.keys.filter { card -> card.cardType == Card.CardType.COLLABORATION }.union(inventory.validationCards.filterKeys { card -> card.cardType == Card.CardType.COLLABORATION }.keys).toSet().size

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
                ECCValidationWay.T4_2 -> {
                    val cardSize = inventory.cards.keys.filter { c -> c.tier == CardData.Tier.LEGEND }.union(inventory.validationCards.filterKeys { k -> k.tier == CardData.Tier.LEGEND }.keys).toSet().size

                    if (cardSize < 2) {
                        builder.append("- Not enough unique collaboration cards! You currently have $cardSize unique card, but 2 cards are required\n")
                    }
                }
                ECCValidationWay.SAME_T4_3 -> {
                    if (inventory.cards.entries.filter { (card, amount) -> card.tier == CardData.Tier.LEGEND && amount >= 3 }.union(inventory.validationCards.entries.filter { (card, pair) -> card.tier == CardData.Tier.LEGEND && pair.second >= 3 }.map { e -> e.key }).toSet().isEmpty()) {
                        builder.append("- You don't have any T4 cards with amount of 3 or over!")
                    }
                }
                ECCValidationWay.NONE -> {}
            }

            return builder.toString()
        }
    }
}
