package mandarin.card.supporter

import common.util.Data
import mandarin.card.supporter.CardData.Tier
import mandarin.card.supporter.filter.BannerFilter
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
}
