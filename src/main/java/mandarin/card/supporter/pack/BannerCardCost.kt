package mandarin.card.supporter.pack

import com.google.gson.JsonObject
import com.google.gson.stream.MalformedJsonException
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card

class BannerCardCost(var banner: Banner, amount: Long) : CardCost(CostType.BANNER, amount) {
    companion object {
        fun readJson(obj: JsonObject) : BannerCardCost {
            if (!obj.has("banner")) {
                throw MalformedJsonException("Invalid BannerCardCost json format")
            }

            val amount = obj.get("amount").asLong
            val banner = Banner.fromName(obj.get("banner").asString)

            return BannerCardCost(banner, amount)
        }
    }

    override fun filter(c: Card): Boolean {
        return banner in c.banner
    }

    override fun getCostName(): String {
        return if (amount > 1) {
            "$amount ${banner.name} cards"
        } else {
            "$amount ${banner.name} card"
        }
    }

    override fun finishJson(obj: JsonObject) {
        obj.addProperty("banner", banner.name)
    }

    fun isInvalid() : Boolean {
        return amount == 0L
    }
}