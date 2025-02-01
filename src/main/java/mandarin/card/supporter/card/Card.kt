package mandarin.card.supporter.card

import com.google.gson.JsonObject
import common.util.Data
import mandarin.card.supporter.CardData.Tier
import mandarin.card.supporter.filter.BannerFilter
import mandarin.packpack.supporter.StaticStore
import java.io.File
import kotlin.collections.contains

class Card(val unitID: Int, val tier: Tier, val name: String, val cardImage: File) {
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

            return card
        }
    }

    var activated = false

    override fun toString(): String {
        return cardInfo()
    }

    fun cardInfo() : String {
        return "Card No.${Data.trio(unitID)} : $name [${getTier()}]"
    }

    fun simpleCardInfo() : String {
        return "Card No.${Data.trio(unitID)} : $name [${getTier()}]"
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
        return tier == Tier.UNCOMMON && unitID !in BannerFilter.Banner.Seasonal.getBannerData() && unitID !in BannerFilter.Banner.Collaboration.getBannerData()
    }

    fun isSeasonalUncommon() : Boolean {
        return tier == Tier.UNCOMMON && unitID in BannerFilter.Banner.Seasonal.getBannerData()
    }

    fun isCollaborationUncommon() : Boolean {
        return tier == Tier.UNCOMMON && unitID in BannerFilter.Banner.Collaboration.getBannerData()
    }

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("id", unitID)
        obj.addProperty("tier", tier.name)
        obj.addProperty("name", name)
        obj.addProperty("activated", activated)

        return obj
    }
}
