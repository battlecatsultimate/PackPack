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
                val u = UserProfile.getBCData().units[c.id]

                u.rarity == 5
            }
            BannerFilter.Banner.BusterExclusives -> {
                c.id in CardData.busters
            }
            else -> {
                c.id in banner.getBannerData()
            }
        }
    }

    override fun getCostName(): String {
        val bannerName = BannerFilter.getBannerName(banner)

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