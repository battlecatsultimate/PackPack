package mandarin.card.supporter

import common.util.Data
import mandarin.card.supporter.CardData.Tier
import java.io.File

class Card(val unitID: Int, val tier: Tier, val name: String, val cardImage: File) {
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
            Tier.COMMON -> "Common"
            Tier.UNCOMMON -> "Uncommon"
            Tier.ULTRA -> "Ultra Rare (Exclusives)"
            Tier.LEGEND -> "Legend Rare"
            Tier.NONE -> "Unknown"
        }
    }
}
