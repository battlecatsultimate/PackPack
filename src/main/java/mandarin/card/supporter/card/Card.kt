package mandarin.card.supporter.card

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import common.util.Data
import mandarin.card.supporter.CardData.Tier
import mandarin.packpack.supporter.StaticStore
import java.io.File
import kotlin.math.abs

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
            val name = obj.get("name").asString
            val tier = Tier.valueOf(obj.get("tier").asString)

            val folderName = when(tier) {
                Tier.SPECIAL -> "Tier 0"
                Tier.COMMON -> "Tier 1"
                Tier.UNCOMMON -> "Tier 2"
                Tier.ULTRA -> "Tier 3"
                Tier.LEGEND -> "Tier 4"
                Tier.NONE -> throw IllegalStateException("E/Card::fromJson - Invalid tier NONE found")
            }

            val cardFile = File("./data/cards/$folderName/${if (tier == Tier.SPECIAL || id >= 0) Data.trio(abs(id)) else id.toString()}-${name}.png")

            if (!cardFile.exists()) {
                StaticStore.logger.uploadLog("W/Card::fromJson - No such ${cardFile.absolutePath} card file found")

                return null
            }

            val card = Card(id, tier, name, cardFile)

            card.activated = obj.has("activated") && obj.get("activated").asBoolean
            card.bcCard = obj.has("bcCard") && obj.get("bcCard").asBoolean
            card.cardType = if (obj.has("cardType")) {
                CardType.valueOf(obj.get("cardType").asString)
            } else {
                CardType.NORMAL
            }

            if (obj.has("banner")) {
                val element = obj.get("banner")

                if (element is JsonPrimitive) {
                    card.banner.add(Banner.fromName(element.asString))
                } else if (element is JsonArray) {
                    element.asJsonArray.forEach { e ->
                        card.banner.add(Banner.fromName(e.asString))
                    }
                }
            }
            
            card.tradable = obj.has("tradable") && obj.get("tradable").asBoolean

            return card
        }
    }

    var activated = true
    var bcCard = false
    var tradable = true
    var cardType = CardType.NORMAL
    val banner = HashSet<Banner>()

    val isRegularUncommon: Boolean
        get() = tier == Tier.UNCOMMON && cardType == CardType.NORMAL
    val isSeasonalUncommon: Boolean
        get() = tier == Tier.UNCOMMON && cardType == CardType.SEASONAL
    val isCollaborationUncommon: Boolean
        get() = tier == Tier.UNCOMMON && cardType == CardType.COLLABORATION

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

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("id", id)
        obj.addProperty("tier", tier.name)
        obj.addProperty("name", name)
        obj.addProperty("activated", activated)
        obj.addProperty("bcCard", bcCard)
        obj.addProperty("tradable", tradable)
        obj.addProperty("cardType", cardType.name)

        val banners = JsonArray()

        banner.forEach { b ->
            banners.add(b.name)
        }

        obj.add("banner", banners)

        return obj
    }
}
