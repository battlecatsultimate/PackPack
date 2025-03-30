package mandarin.card.supporter.pack

import com.google.gson.JsonObject
import com.google.gson.stream.MalformedJsonException
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.CardData

class TierCardCost(var tier: CardPack.CardType, amount: Long) : CardCost(CostType.TIER, amount) {
    companion object {
        fun readJson(obj: JsonObject) : TierCardCost {
            if (!obj.has("tier")) {
                throw MalformedJsonException("Invalid TierCardCost json format")
            }

            val amount = obj.get("amount").asLong
            val tiers = CardPack.CardType.valueOf(obj.get("tier").asString)

            return TierCardCost(tiers, amount)
        }
    }

    override fun filter(c: Card) : Boolean {
        return when(tier) {
            CardPack.CardType.T1 -> c.tier == CardData.Tier.COMMON
            CardPack.CardType.T2 -> c.tier == CardData.Tier.UNCOMMON
            CardPack.CardType.REGULAR -> c.isRegularUncommon
            CardPack.CardType.SEASONAL -> c.isSeasonalUncommon
            CardPack.CardType.COLLABORATION -> c.isCollaborationUncommon
            CardPack.CardType.T3 -> c.tier == CardData.Tier.ULTRA
            CardPack.CardType.T4 -> c.tier == CardData.Tier.LEGEND
        }
    }

    override fun finishJson(obj: JsonObject) {
        obj.addProperty("tier", tier.name)
    }

    override fun getCostName(): String {
        val tierName = when(tier) {
            CardPack.CardType.T1 -> "Tier 1 [Common]"
            CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
            CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
            CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
            CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
        }

        return if (amount > 1) {
            "$amount $tierName cards"
        } else {
            "$amount $tierName cards"
        }
    }

    fun isInvalid() : Boolean {
        return amount == 0L
    }
}