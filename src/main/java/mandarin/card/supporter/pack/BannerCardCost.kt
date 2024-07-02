package mandarin.card.supporter.pack

import com.google.gson.JsonObject
import com.google.gson.stream.MalformedJsonException
import common.pack.UserProfile
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.filter.BannerFilter

class BannerCardCost(var banner: BannerFilter.Banner, amount: Long) : CardCost(CostType.BANNER, amount) {
    init {
        when(banner) {
            BannerFilter.Banner.Seasonal,
            BannerFilter.Banner.Collaboration,
            BannerFilter.Banner.LegendRare -> throw IllegalStateException("This banner $banner is meaningless banner as it can be handled by other card cost type")
            else -> {}
        }
    }

    companion object {
        fun readJson(obj: JsonObject) : BannerCardCost {
            if (!obj.has("banner")) {
                throw MalformedJsonException("Invalid BannerCardCost json format")
            }

            val amount = obj.get("amount").asLong
            val banner = BannerFilter.Banner.valueOf(obj.get("banner").asString)

            return BannerCardCost(banner, amount)
        }
    }

    override fun filter(c: Card): Boolean {
        return when (banner) {
            BannerFilter.Banner.LegendRare -> {
                val u = UserProfile.getBCData().units[c.unitID]

                u.rarity == 5
            }
            BannerFilter.Banner.BusterExclusives -> {
                c.unitID in CardData.busters
            }
            else -> {
                c.unitID in banner.getBannerData()
            }
        }
    }

    override fun getCostName(): String {
        val bannerName = when(banner) {
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
            BannerFilter.Banner.LegendRare ->  throw IllegalStateException("E/BannerCostHolder::getContents - Invalid banner $banner found")

            BannerFilter.Banner.CheetahT1 -> "Cheetah Tier 1"
            BannerFilter.Banner.CheetahT2 -> "Cheetah Tier 2"
            BannerFilter.Banner.CheetahT3 -> "Cheetah Tier 3"
            BannerFilter.Banner.CheetahT4 -> "Cheetah Tier 4"
        }

        return if (amount > 1) {
            "$amount $bannerName cards"
        } else {
            "$amount $bannerName card"
        }
    }

    override fun finishJson(obj: JsonObject) {
        obj.addProperty("banner", banner.name)
    }

    fun isInvalid() : Boolean {
        return amount == 0L
    }
}