package mandarin.card.supporter.card

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import common.util.Data
import mandarin.card.supporter.CardData
import mandarin.card.supporter.CardData.Tier
import mandarin.card.supporter.filter.BannerFilter
import mandarin.packpack.supporter.StaticStore
import java.io.File
import kotlin.collections.contains

class Card(var id: Int, var tier: Tier, var name: String, var cardImage: File) {
    enum class CardType {
        NORMAL,
        COLLABORATION,
        SEASONAL,
        APRIL_FOOL
    }

    companion object {
        fun fromJson(obj: JsonObject) : Card? {
            if (!obj.has("id") || !obj.has("tier") || !obj.has("name")) {
                return null
            }

            val id = obj.get("id").asInt
            val tier = Tier.valueOf(obj.get("tier").asString)

            val folderName = when(tier) {
                Tier.SPECIAL -> "Tier 0"
                Tier.COMMON -> "Tier 1"
                Tier.UNCOMMON -> "Tier 2"
                Tier.ULTRA -> "Tier 3"
                Tier.LEGEND -> "Tier 4"
                Tier.NONE -> throw IllegalStateException("E/Card::fromJson - Invalid tier NONE found")
            }

            val cardFile = File("./cards/$folderName/${Data.trio(id)}.png")

            if (!cardFile.exists()) {
                StaticStore.logger.uploadLog("W/Card::fromJson - No such ${cardFile.absolutePath} card file found")

                return null
            }

            val name = obj.get("name").asString

            val card = Card(id, tier, name, cardFile)

            card.activated = obj.has("activated") && obj.get("activated").asBoolean
            card.bcCard = obj.has("bcCard") && obj.get("bcCard").asBoolean
            card.cardType = if (obj.has("cardType")) {
                CardType.valueOf(obj.get("cardType").asString)
            } else {
                CardType.NORMAL
            }

            card.banner = if (obj.has("banner")) {
                val banner = obj.get("banner")

                if (banner is JsonNull) {
                    Banner.NONE
                } else {
                    val bannerName = banner.asString

                    CardData.banners.find { b -> b.name == bannerName } ?: Banner.NONE
                }
            } else {
                Banner.NONE
            }

            return card
        }
    }

    var activated = false
    var bcCard = false
    var cardType = CardType.NORMAL
    var banner = Banner.NONE

    override fun toString(): String {
        return cardInfo()
    }

    fun cardInfo() : String {
        return "Card No.${Data.trio(id)} : $name [${getTier()}]"
    }

    fun simpleCardInfo() : String {
        return "Card No.${Data.trio(id)} : $name [${getTier()}]"
    }

    fun getTier() : String {
        return when(tier) {
            Tier.SPECIAL -> "Special"
            Tier.COMMON -> "Common"
            Tier.UNCOMMON -> "Uncommon"
            Tier.ULTRA -> "Ultra Rare (Exclusives)"
            Tier.LEGEND -> "Legend Rare"
            Tier.NONE -> "Unknown"
        }
    }

    fun isRegularUncommon() : Boolean {
        return tier == Tier.UNCOMMON && id !in BannerFilter.Banner.Seasonal.getBannerData() && id !in BannerFilter.Banner.Collaboration.getBannerData()
    }

    fun isSeasonalUncommon() : Boolean {
        return tier == Tier.UNCOMMON && id in BannerFilter.Banner.Seasonal.getBannerData()
    }

    fun isCollaborationUncommon() : Boolean {
        return tier == Tier.UNCOMMON && id in BannerFilter.Banner.Collaboration.getBannerData()
    }

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("id", id)
        obj.addProperty("tier", tier.name)
        obj.addProperty("name", name)
        obj.addProperty("activated", activated)
        obj.addProperty("bcCard", bcCard)
        obj.addProperty("cardType", cardType.name)

        if (banner === Banner.NONE) {
            obj.add("banner", null)
        } else {
            obj.addProperty("banner", banner.name)
        }

        return obj
    }
}
