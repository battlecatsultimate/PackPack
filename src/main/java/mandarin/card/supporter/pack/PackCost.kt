package mandarin.card.supporter.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.packpack.supporter.EmojiStore

class PackCost(
    var catFoods: Long,
    var platinumShards: Long,
    val cardsCosts: ArrayList<CardCost>,
    val roles: ArrayList<String>
) {
    companion object {
        fun fromJson(obj: JsonObject) : PackCost {
            val cost = PackCost(0L, 0L, ArrayList(), ArrayList())

            if (obj.has("catFoods")) {
                cost.catFoods = obj.get("catFoods").asLong
            }

            if (obj.has("platinumShards")) {
                cost.platinumShards = obj.get("platinumShards").asLong
            }

            if (obj.has("cardCosts")) {
                val arr = obj.getAsJsonArray("cardCosts")

                arr.forEach { e ->
                    val o = e.asJsonObject

                    cost.cardsCosts.add(CardCost.fromJson(o))
                }
            }

            if (obj.has("roles")) {
                val arr = obj.getAsJsonArray("roles")

                arr.forEach { e ->
                    cost.roles.add(e.asString)
                }
            }

            return cost
        }
    }

    fun affordable(inventory: Inventory, id: Long) : Boolean {
        val queuedCatFoods = CardData.sessions.filter { session -> id in session.member }.sumOf { session ->
            val index = session.member.indexOf(id)

            session.suggestion[index].catFood
        }

        if (inventory.catFoods - queuedCatFoods < catFoods)
            return false

        if (inventory.platinumShard < platinumShards)
            return false

        val bannerCards = HashMap<BannerFilter.Banner, Long>()

        cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
            val existingCards = inventory.cards.entries.sumOf { (card, amount) -> if (card.unitID in cost.banner.getBannerData()) amount else 0 } - (bannerCards[cost.banner] ?: 0)

            if (existingCards < cost.amount)
                return false

            bannerCards[cost.banner] = (bannerCards[cost.banner] ?: 0L) + cost.amount
        }

        cardsCosts.filterIsInstance<TierCardCost>().forEach { cost ->
            val existingCards = inventory.cards.entries.sumOf { (card, amount) ->
                val match = when(cost.tier) {
                    CardPack.CardType.T1 -> card.tier == CardData.Tier.COMMON
                    CardPack.CardType.T2 -> card.tier == CardData.Tier.UNCOMMON
                    CardPack.CardType.REGULAR -> card.isRegularUncommon()
                    CardPack.CardType.SEASONAL -> card.isSeasonalUncommon()
                    CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon()
                    CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                    CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                }

                if (match)
                    amount
                else
                    0
            } - bannerCards.entries.sumOf { (banner, amount) -> if (banner.getCardType() == cost.tier) amount else 0 }

            if (existingCards < cost.amount)
                return false
        }

        return true
    }

    fun getReason(inventory: Inventory, id: Long) : String {
        val builder = StringBuilder()

        val queuedCatFoods = CardData.sessions.filter { session -> id in session.member }.sumOf { session ->
            val index = session.member.indexOf(id)

            session.suggestion[index].catFood
        }

        if (inventory.catFoods - queuedCatFoods < catFoods) {
            if (inventory.catFoods >= catFoods) {
                builder.append("- You don't have enough cat foods because you already suggested your cat food in other trading sessions. You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods - queuedCatFoods} free\n")
            } else {
                builder.append("- You don't have enough cat foods. You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}\n")
            }
        }

        if (inventory.platinumShard < platinumShards) {
            builder.append("- You don't have enough platinum shards. You currently have ${inventory.platinumShard} ${EmojiStore.ABILITY["SHARD"]?.formatted}\n")
        }

        val bannerCards = HashMap<BannerFilter.Banner, Long>()

        cardsCosts.filterIsInstance<BannerCardCost>().forEach { cost ->
            val totalCards = inventory.cards.entries.sumOf { (card, amount) -> if (card.unitID in cost.banner.getBannerData()) amount else 0 }
            val existingCards = totalCards - (bannerCards[cost.banner] ?: 0)

            val bannerName = when(cost.banner) {
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
                BannerFilter.Banner.LegendRare ->  throw IllegalStateException("E/BannerCostHolder::getContents - Invalid banner ${cost.banner} found")
            }

            if (existingCards < cost.amount) {
                builder.append("- You don't have enough $bannerName cards. You currently have $totalCards card(s)\n")
            }

            bannerCards[cost.banner] = (bannerCards[cost.banner] ?: 0L) + cost.amount
        }

        cardsCosts.filterIsInstance<TierCardCost>().forEach { cost ->
            val totalCards = inventory.cards.entries.sumOf { (card, amount) ->
                val match = when(cost.tier) {
                    CardPack.CardType.T1 -> card.tier == CardData.Tier.COMMON
                    CardPack.CardType.T2 -> card.tier == CardData.Tier.UNCOMMON
                    CardPack.CardType.REGULAR -> card.isRegularUncommon()
                    CardPack.CardType.SEASONAL -> card.isSeasonalUncommon()
                    CardPack.CardType.COLLABORATION -> card.isCollaborationUncommon()
                    CardPack.CardType.T3 -> card.tier == CardData.Tier.ULTRA
                    CardPack.CardType.T4 -> card.tier == CardData.Tier.LEGEND
                }

                if (match)
                    amount
                else
                    0
            }

            val existingCards = totalCards - bannerCards.entries.sumOf { (banner, amount) -> if (banner.getCardType() == cost.tier) amount else 0 }

            val tierName = when(cost.tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            if (existingCards < cost.amount) {
                builder.append("- You don't have enough $tierName cards if you pay other costs. You currently have $totalCards card(s)\n")
            }
        }

        return builder.toString()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("catFoods", catFoods)
        obj.addProperty("platinumShards", platinumShards)

        val cardArr = JsonArray()

        cardsCosts.forEach {
            cardArr.add(it.asJson())
        }

        obj.add("cardCosts", cardArr)

        val roleArr = JsonArray()

        roles.forEach {
            roleArr.add(it)
        }

        obj.add("roles", roleArr)

        return obj
    }
}