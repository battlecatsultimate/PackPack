package mandarin.card.supporter.pack

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData

class CardPayContainer(val cost: CardCost) {
    val pickedCards = ArrayList<Card>()

    fun paid() : Boolean {
        when(cost) {
            is BannerCardCost -> {
                return pickedCards.filter { c -> c.unitID in cost.banner.getBannerData() }.size.toLong() == cost.amount
            }
            is TierCardCost -> {
                return pickedCards.filter { c ->
                    when(cost.tier) {
                        CardPack.CardType.T1 -> c.tier == CardData.Tier.COMMON
                        CardPack.CardType.T2 -> c.tier == CardData.Tier.UNCOMMON
                        CardPack.CardType.REGULAR -> c.isRegularUncommon()
                        CardPack.CardType.SEASONAL -> c.isSeasonalUncommon()
                        CardPack.CardType.COLLABORATION -> c.isCollaborationUncommon()
                        CardPack.CardType.T3 -> c.tier == CardData.Tier.ULTRA
                        CardPack.CardType.T4 -> c.tier == CardData.Tier.LEGEND
                    }
                }.size.toLong() == cost.amount
            }
            is SpecificCardCost -> {
                return pickedCards.filter { c -> c in cost.cards }.size.toLong() == cost.amount
            }
        }

        return false
    }
}