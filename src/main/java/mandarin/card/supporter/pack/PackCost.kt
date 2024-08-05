package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
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

            if (cost.cardsCosts.any { cost -> cost is SpecificCardCost } && cost.cardsCosts.size != 1) {
                throw IllegalStateException("E/PackCost::fromJson - There must be only ONE specific card cost!")
            }

            return cost
        }
    }

    fun affordable(inventory: Inventory) : Boolean {
        if (inventory.actualCatFood < catFoods)
            return false

        if (inventory.platinumShard < platinumShards)
            return false

        if (cardsCosts.any { cost -> cost is SpecificCardCost }) {
            val cost = cardsCosts[0] as SpecificCardCost
            val existingCards = inventory.cards.entries.filter { (card, _) -> card in cost.cards }.sumOf { it.value }

            if (existingCards < cost.amount)
                return false
        } else {
            val bannerCards = HashMap<BannerFilter.Banner, Long>()

            cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
                val existingCards = inventory.cards.entries.sumOf { (card, amount) -> if (card.unitID in cost.banner.getBannerData()) amount else 0 } - (bannerCards[cost.banner] ?: 0)

                if (existingCards < cost.amount)
                    return false

                bannerCards[cost.banner] = (bannerCards[cost.banner] ?: 0L) + cost.amount
            }

            cardsCosts.filterIsInstance<TierCardCost>().forEach { cost ->
                val existingCards = inventory.cards.entries.sumOf { (card, amount) ->
                    val match = when(cost.tier) {
                        CardPack.CardType.T1 -> card.tier == CardData.Tier.COMMON
                        CardPack.CardType.T2 -> card.tier == CardData.Tier.UNCOMMON
                        CardPack.CardType.REGULAR -> card.isRegularUncommon()
                        CardPack.CardType.SEASONAL -> card.isSeasonalUncommon()
                        CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon()
                        CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                        CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                    }

                    if (match)
                        amount
                    else
                        0
                } - bannerCards.entries.sumOf { (banner, amount) -> if (banner.getCardType() == cost.tier) amount else 0 }

                if (existingCards < cost.amount)
                    return false
            }
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

        if (cardsCosts.any { cost -> cost is SpecificCardCost }) {
            val cost = cardsCosts[0] as SpecificCardCost
            val existingCards = inventory.cards.entries.filter { (card, _) -> card in cost.cards }.sumOf { it.value }

            if (existingCards < cost.amount)
                builder.append("- You don't have enough cards of these groups : ${cost.getCostName()}")
        } else {
            val bannerCards = HashMap<BannerFilter.Banner, Long>()

            cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
                val totalCards = inventory.cards.entries.sumOf { (card, amount) -> if (card.unitID in cost.banner.getBannerData()) amount else 0 }
                val existingCards = totalCards - (bannerCards[cost.banner] ?: 0)

                val bannerName = BannerFilter.getBannerName(cost.banner)

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
                        CardPack.CardType.REGULAR -> card.isRegularUncommon()
                        CardPack.CardType.SEASONAL -> card.isSeasonalUncommon()
                        CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon()
                        CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                        CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                    }

                    if (match)
                        amount
                    else
                        0
                }

                val existingCards = totalCards - bannerCards.entries.sumOf { (banner, amount) -> if (banner.getCardType() == cost.tier) amount else 0 }

                val tierName = when(cost.tier) {
                    CardPack.CardType.T1 -> "Tier 1 [Common]"
                    CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                    CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                    CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                    CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                    CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                    CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
                }

                if (existingCards < cost.amount) {
                    builder.append("- You don't have enough $tierName cards if you pay other costs. You currently have $totalCards card(s)\n")
                }
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