package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Banner
import mandarin.packpack.supporter.EmojiStore

class PackCost(
    var catFoods: Long,
    var platinumShards: Long,
    val cardsCosts: ArrayList<CardCost>,
    val roles: ArrayList<String>
) {
    companion object {
        fun fromJson(obj: JsonObject) : PackCost {
            val cost = PackCost(0L, 0L, ArrayList(), ArrayList())

            if (obj.has("catFoods")) {
                cost.catFoods = obj.get("catFoods").asLong
            }

            if (obj.has("platinumShards")) {
                cost.platinumShards = obj.get("platinumShards").asLong
            }

            if (obj.has("cardCosts")) {
                val arr = obj.getAsJsonArray("cardCosts")

                arr.forEach { e ->
                    val o = e.asJsonObject

                    cost.cardsCosts.add(CardCost.fromJson(o))
                }
            }

            if (obj.has("roles")) {
                val arr = obj.getAsJsonArray("roles")

                arr.forEach { e ->
                    cost.roles.add(e.asString)
                }
            }

            val costFilter = cost.cardsCosts.filter { cost -> cost is BannerCardCost || cost is SpecificCardCost || cost is TierCardCost }

            costFilter.forEach { c ->
                when (c) {
                    is BannerCardCost -> {
                        val currentCardPool = c.convertCardCostToCards().toSet()

                        costFilter.forEach { otherCost ->
                            if (otherCost === c)
                                return@forEach

                            val compareCardPool = otherCost.convertCardCostToCards().toSet()

                            if (compareCardPool.intersect(currentCardPool).isNotEmpty()) {
                                throw IllegalStateException("E/PackCost::fromJson - Intersecting card pool has been found : \n" +
                                        "\n" +
                                        "${currentCardPool.joinToString("\n")}\n" +
                                        "\n" +
                                        compareCardPool.joinToString("\n")
                                )
                            }
                        }
                    }
                }
            }

            return cost
        }
    }

    fun affordable(inventory: Inventory) : Boolean {
        if (inventory.actualCatFood < catFoods)
            return false

        if (inventory.platinumShard < platinumShards)
            return false

        cardsCosts.filterIsInstance<SpecificCardCost>().forEach { cost ->
            val existingCards = inventory.cards.entries.filter { (card, _) -> card in cost.cards }.sumOf { it.value }

            if (existingCards < cost.amount)
                return false
        }

        val bannerCards = HashMap<Banner, Long>()

        cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
            val collectedCards = CardData.cards.filter { c -> cost.banner in c.banner }

            val existingCards = inventory.cards.entries.sumOf { (card, amount) -> if (card in collectedCards) amount else 0 } - (bannerCards[cost.banner] ?: 0)

            if (existingCards < cost.amount)
                return false

            bannerCards[cost.banner] = (bannerCards[cost.banner] ?: 0L) + cost.amount
        }

        cardsCosts.filterIsInstance<TierCardCost>().forEach { cost ->
            val existingCards = inventory.cards.entries.sumOf { (card, amount) ->
                val match = when(cost.tier) {
                    CardPack.CardType.T1 -> card.tier == CardData.Tier.COMMON
                    CardPack.CardType.T2 -> card.tier == CardData.Tier.UNCOMMON
                    CardPack.CardType.REGULAR -> card.isRegularUncommon
                    CardPack.CardType.SEASONAL -> card.isSeasonalUncommon
                    CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon
                    CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                    CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                }

                if (match)
                    amount
                else
                    0
            }

            if (existingCards < cost.amount)
                return false
        }

        return true
    }

    fun getReason(inventory: Inventory) : String {
        val builder = StringBuilder()

        if (inventory.actualCatFood < catFoods) {
            if (inventory.catFoods >= catFoods) {
                builder.append("- You don't have enough cat foods because you already suggested your cat food in other trading sessions. You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood} free\n")
            } else {
                builder.append("- You don't have enough cat foods. You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}\n")
            }
        }

        if (inventory.platinumShard < platinumShards) {
            builder.append("- You don't have enough platinum shards. You currently have ${inventory.platinumShard} ${EmojiStore.ABILITY["SHARD"]?.formatted}\n")
        }

        cardsCosts.filterIsInstance<SpecificCardCost>().forEach { cost ->
            val existingCards = inventory.cards.entries.filter { (card, _) -> card in cost.cards }.sumOf { it.value }

            if (existingCards < cost.amount)
                builder.append("- You don't have enough cards of these groups : ${cost.getCostName()}")
        }

        val bannerCards = HashMap<Banner, Long>()

        cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
            val collectedCards = CardData.cards.filter { c -> cost.banner in c.banner }

            val totalCards = inventory.cards.entries.sumOf { (card, amount) -> if (card in collectedCards) amount else 0 }
            val existingCards = totalCards - (bannerCards[cost.banner] ?: 0)

            val bannerName = cost.banner.name

            if (existingCards < cost.amount) {
                builder.append("- You don't have enough $bannerName cards. You currently have $totalCards card(s)\n")
            }

            bannerCards[cost.banner] = (bannerCards[cost.banner] ?: 0L) + cost.amount
        }

        cardsCosts.filterIsInstance<TierCardCost>().forEach { cost ->
            val totalCards = inventory.cards.entries.sumOf { (card, amount) ->
                val match = when(cost.tier) {
                    CardPack.CardType.T1 -> card.tier == CardData.Tier.COMMON
                    CardPack.CardType.T2 -> card.tier == CardData.Tier.UNCOMMON
                    CardPack.CardType.REGULAR -> card.isRegularUncommon
                    CardPack.CardType.SEASONAL -> card.isSeasonalUncommon
                    CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon
                    CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                    CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                }

                if (match)
                    amount
                else
                    0
            }

            val tierName = when(cost.tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            if (totalCards < cost.amount) {
                builder.append("- You don't have enough $tierName cards if you pay other costs. You currently have $totalCards card(s)\n")
            }
        }

        return builder.toString()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("catFoods", catFoods)
        obj.addProperty("platinumShards", platinumShards)

        val cardArr = JsonArray()

        cardsCosts.forEach {
            cardArr.add(it.asJson())
        }

        obj.add("cardCosts", cardArr)

        val roleArr = JsonArray()

        roles.forEach {
            roleArr.add(it)
        }

        obj.add("roles", roleArr)

        return obj
    }
}