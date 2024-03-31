package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.filter.BannerFilter

class CardGroupData(
    val types: ArrayList<CardPack.CardType>,
    val extra: ArrayList<BannerFilter.Banner>
) {
    companion object {
        fun fromJson(obj: JsonObject) : CardGroupData {
            val groupData = CardGroupData(ArrayList(), ArrayList())

            if (obj.has("types")) {
                val arr = obj.getAsJsonArray("types")

                arr.forEach { e ->
                    groupData.types.add(CardPack.CardType.valueOf(e.asString))
                }
            }

            if (obj.has("extra")) {
                val arr = obj.getAsJsonArray("extra")

                arr.forEach { e ->
                    groupData.extra.add(BannerFilter.Banner.valueOf(e.asString))
                }
            }

            return groupData
        }
    }

    fun gatherPools() : List<Card> {
        val result = ArrayList<Card>()

        for (type in types) {
            result.addAll(
                when (type) {
                    CardPack.CardType.T1 -> CardData.cards.filter { c -> c.tier == CardData.Tier.COMMON }
                    CardPack.CardType.T2 -> CardData.appendUncommon()
                    CardPack.CardType.REGULAR -> CardData.cards.filter { c -> c.isRegularUncommon() }
                    CardPack.CardType.SEASONAL -> CardData.cards.filter { c -> c.isSeasonalUncommon() }
                    CardPack.CardType.COLLABORATION -> CardData.cards.filter { c -> c.isCollaborationUncommon() }
                    CardPack.CardType.T3 -> CardData.appendUltra()
                    CardPack.CardType.T4 -> CardData.appendLR()
                }
            )
        }

        for (banner in extra) {
            result.addAll(banner.getBannerData().mapNotNull { id -> CardData.cards.find { c -> c.unitID == id } })
        }

        result.removeIf { c -> c.unitID < 0 }

        return result.toSet().toList()
    }

    fun getName() : String {
        val builder = StringBuilder()

        for (i in types.indices) {
            val typeName = when(types[i]) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            builder.append(typeName)

            if (i < types.size - 1) {
                builder.append(", ")
            }
        }

        if (types.isNotEmpty() && extra.isNotEmpty()) {
            builder.append(", ")
        }

        for (i in extra.indices) {
            val bannerName = when(extra[i]) {
                BannerFilter.Banner.DarkHeroes -> "Dark Heroes"
                BannerFilter.Banner.DragonEmperors -> "Dragone Emperors"
                BannerFilter.Banner.Dynamites -> "Dynamites"
                BannerFilter.Banner.ElementalPixies -> "Elemental Pixies"
                BannerFilter.Banner.GalaxyGals -> "Galaxy Girls"
                BannerFilter.Banner.IronLegion -> "Iron Legion"
                BannerFilter.Banner.SengokuWargods -> "Sengoku Wargods"
                BannerFilter.Banner.TheNekolugaFamily -> "The Nekoluga Family"
                BannerFilter.Banner.UltraSouls -> "Ultra Souls"
                BannerFilter.Banner.GirlsAndMonsters -> "Girls And Monsters"
                BannerFilter.Banner.TheAlimighties -> "The Almighties"
                BannerFilter.Banner.EpicfestExclusives -> "Epicfest Exclusives"
                BannerFilter.Banner.UberfestExclusives -> "Uberfest Exclusives"
                BannerFilter.Banner.OtherExclusives -> "Other Exclusives"
                BannerFilter.Banner.BusterExclusives -> "Buster Exclusives"
                BannerFilter.Banner.Valentine -> "Valentine's Day"
                BannerFilter.Banner.Whiteday -> "White Day"
                BannerFilter.Banner.Easter -> "Easter"
                BannerFilter.Banner.JuneBride -> "June Bride"
                BannerFilter.Banner.SummerGals -> "Summer Gals"
                BannerFilter.Banner.Halloweens -> "Halloween"
                BannerFilter.Banner.XMas -> "X-Max"
                BannerFilter.Banner.Bikkuriman -> "Bikkuriman"
                BannerFilter.Banner.CrashFever -> "Crash Fever"
                BannerFilter.Banner.Fate -> "Fate Stay/Night"
                BannerFilter.Banner.Miku -> "Hatsune Miku"
                BannerFilter.Banner.MercStroia -> "Merc Storia"
                BannerFilter.Banner.Evangelion -> "Evangelion"
                BannerFilter.Banner.PowerPro -> "Power Pro Baseball"
                BannerFilter.Banner.Ranma -> "Ranma 1/2"
                BannerFilter.Banner.RiverCity -> "River City"
                BannerFilter.Banner.ShoumetsuToshi -> "Annihilated City"
                BannerFilter.Banner.StreetFighters -> "Street Fighters"
                BannerFilter.Banner.MolaSurvive -> "Survive! Mola Mola!"
                BannerFilter.Banner.MetalSlug -> "Metal Slug"
                BannerFilter.Banner.PrincessPunt -> "Princess Punt"
                BannerFilter.Banner.Collaboration,
                BannerFilter.Banner.Seasonal,
                BannerFilter.Banner.LegendRare ->  throw IllegalStateException("E/BannerCostHolder::getContents - Invalid banner ${extra[i]} found")

                BannerFilter.Banner.CheetahT1 -> "Tier 1 [Common]"
                BannerFilter.Banner.CheetahT2 -> "Tier 2 [Uncommon]"
                BannerFilter.Banner.CheetahT3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                BannerFilter.Banner.CheetahT4 -> "Tier 4 [Legend Rare]"
            }

            builder.append(bannerName)

            if (i < extra.size - 1) {
                builder.append(", ")
            }
        }

        return builder.toString()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        val typeArr = JsonArray()

        types.forEach { tier ->
            typeArr.add(tier.name)
        }

        obj.add("types", typeArr)

        val extraArr = JsonArray()

        extra.forEach { banner ->
            extraArr.add(banner.name)
        }

        obj.add("extra", extraArr)

        return obj
    }
}