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

    enum class Banner(val tier: CardData.Tier, val category: Int) {
        DarkHeroes(CardData.Tier.COMMON, 0),
        DragonEmperors(CardData.Tier.COMMON, 1),
        Dynamites(CardData.Tier.COMMON, 2),
        ElementalPixies(CardData.Tier.COMMON, 3),
        GalaxyGals(CardData.Tier.COMMON, 4),
        IronLegion(CardData.Tier.COMMON, 5),
        SengokuWargods(CardData.Tier.COMMON, 6),
        TheNekolugaFamily(CardData.Tier.COMMON, 7),
        UltraSouls(CardData.Tier.COMMON, 8),
        GirlsAndMonsters(CardData.Tier.UNCOMMON, 0),
        TheAlimighties(CardData.Tier.UNCOMMON, 1),
        Valentine(CardData.Tier.UNCOMMON,4),
        Whiteday(CardData.Tier.UNCOMMON, 5),
        Easter(CardData.Tier.UNCOMMON, 6),
        JuneBride(CardData.Tier.UNCOMMON, 7),
        SummerGals(CardData.Tier.UNCOMMON, 8),
        Halloweens(CardData.Tier.UNCOMMON, 9),
        XMas(CardData.Tier.UNCOMMON, 10),
        Bikkuriman(CardData.Tier.UNCOMMON, 11),
        CrashFever(CardData.Tier.UNCOMMON, 12),
        Fate(CardData.Tier.UNCOMMON, 13),
        Miku(CardData.Tier.UNCOMMON, 14),
        MercStroia(CardData.Tier.UNCOMMON, 15),
        Evangelion(CardData.Tier.UNCOMMON, 16),
        PowerPro(CardData.Tier.UNCOMMON, 17),
        Ranma(CardData.Tier.UNCOMMON, 18),
        RiverCity(CardData.Tier.UNCOMMON, 19),
        ShoumetsuToshi(CardData.Tier.UNCOMMON, 20),
        StreetFighters(CardData.Tier.UNCOMMON, 21),
        MolaSurvive(CardData.Tier.UNCOMMON, 22),
        MetalSlug(CardData.Tier.UNCOMMON, 23),
        PrincessPunt(CardData.Tier.UNCOMMON, 24),
        EpicfestExclusives(CardData.Tier.ULTRA, 0),
        UberfestExclusives(CardData.Tier.ULTRA, 1),
        OtherExclusives(CardData.Tier.ULTRA, 2),
        Collaboration(CardData.Tier.UNCOMMON, 3),
        Seasonal(CardData.Tier.UNCOMMON, 2),
        LegendRare(CardData.Tier.NONE, -1),
        BusterExclusives(CardData.Tier.NONE, -1),
        CheetahT1(CardData.Tier.COMMON, 9),
        CheetahT2(CardData.Tier.UNCOMMON, 25),
        CheetahT3(CardData.Tier.ULTRA, 5),
        CheetahT4(CardData.Tier.LEGEND, 2);

        fun getBannerData() : Array<Int> {
            return CardData.bannerData[tier.ordinal][category]
        }

        fun getCardType() : CardPack.CardType {
            return if (tier == CardData.Tier.UNCOMMON) {
                val banners = getBannerData()

                if (Seasonal.getBannerData().any { id -> id in banners })
                    CardPack.CardType.SEASONAL
                else if (Collaboration.getBannerData().any { id -> id in banners })
                    CardPack.CardType.COLLABORATION
                else
                    CardPack.CardType.T2
            } else {
                when(tier) {
                    CardData.Tier.COMMON -> CardPack.CardType.T1
                    CardData.Tier.ULTRA -> CardPack.CardType.T3
                    CardData.Tier.LEGEND -> CardPack.CardType.T4
                    else -> throw IllegalStateException("E/BannerFilter::getTier - Unknown tier $tier")
                }
            }
        }
    }

    override fun filter(card: Card): Boolean {
        return when (banner) {
            Banner.LegendRare -> {
                if (card.unitID < 0)
                    return false

                val u = UserProfile.getBCData().units[card.unitID] ?: return false

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