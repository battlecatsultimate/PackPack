package mandarin.card.supporter.filter

import common.pack.UserProfile
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.CardPack

class BannerFilter(private val banner: Banner, amount: Int, name: String) : Filter(amount, name) {
    companion object {
        val pureBanners = Banner.entries.filter { banner ->
            banner != Banner.Seasonal &&
            banner != Banner.LegendRare &&
            banner != Banner.Collaboration
        }
    }

    enum class Banner(val tier: Int, val category: Int) {
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
        Valentine(1,4),
        Whiteday(1, 5),
        Easter(1, 6),
        JuneBride(1, 7),
        SummerGals(1, 8),
        Halloweens(1, 9),
        XMas(1, 10),
        Bikkuriman(1, 11),
        CrashFever(1, 12),
        Fate(1, 13),
        Miku(1, 14),
        MercStroia(1, 15),
        Evangelion(1, 16),
        PowerPro(1, 17),
        Ranma(1, 18),
        RiverCity(1, 19),
        ShoumetsuToshi(1, 20),
        StreetFighters(1, 21),
        MolaSurvive(1, 22),
        MetalSlug(1, 23),
        PrincessPunt(1, 24),
        EpicfestExclusives(2, 0),
        UberfestExclusives(2, 1),
        OtherExclusives(2, 2),
        Collaboration(1, 3),
        Seasonal(1, 2),
        LegendRare(-1, -1),
        BusterExclusives(-1, -1);

        fun getBannerData() : Array<Int> {
            return CardData.bannerData[tier][category]
        }

        fun getCardType() : CardPack.CardType {
            return if (tier == 1) {
                val banners = getBannerData()

                if (Seasonal.getBannerData().any { id -> id in banners })
                    CardPack.CardType.SEASONAL
                else if (Collaboration.getBannerData().any { id -> id in banners })
                    CardPack.CardType.COLLABORATION
                else
                    CardPack.CardType.T2
            } else {
                when(tier) {
                    0 -> CardPack.CardType.T1
                    2 -> CardPack.CardType.T3
                    3 -> CardPack.CardType.T4
                    else -> throw IllegalStateException("E/BannerFilter::getTier - Unknown tier $tier")
                }
            }
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
            else -> {
                card.unitID in banner.getBannerData()
            }
        }
    }
}