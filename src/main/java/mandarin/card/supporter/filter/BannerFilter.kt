package mandarin.card.supporter.filter

import common.pack.UserProfile
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData

class BannerFilter(private val banner: Banner, amount: Int, name: String) : Filter(amount, name) {
    enum class Banner(private val tier: Int, private val category: Int) {
        DarkHeroes(0, 0),
        DragonEmperors(0, 1),
        Dynamites(0, 2),
        ElementalPixies(0, 3),
        GalaxyGals(0, 4),
        IronLegion(0, 5),
        SengokuWargods(0, 6),
        TheNekolugaFamily(0, 7),
        UltraSouls(0, 8),
        GirlsAndMonsters(1, 0),
        TheAlimighties(1, 1),
        EpicfestExclusives(2, 0),
        UberfestExclusives(2, 1),
        OtherExclusives(2, 2),
        Collaboration(-1, -1),
        Seasonal(-1, -1),
        LegendRare(-1, -1),
        BusterExclusives(-1, -1);

        fun getBannerData() : Array<Int> {
            return CardData.bannerData[tier][category]
        }
    }

    override fun filter(card: Card): Boolean {
        return when (banner) {
            Banner.LegendRare -> {
                val u = UserProfile.getBCData().units[card.unitID]

                u.rarity == 5
            }
            Banner.BusterExclusives -> {
                card.unitID in CardData.busters
            }
            Banner.Collaboration -> {
                (9..21).any { card.unitID in CardData.bannerData[CardData.Tier.UNCOMMON.ordinal][it] }
            }
            Banner.Seasonal -> {
                (2..8).any { card.unitID in CardData.bannerData[CardData.Tier.UNCOMMON.ordinal][it] }
            }
            else -> {
                card.unitID in banner.getBannerData()
            }
        }
    }
}