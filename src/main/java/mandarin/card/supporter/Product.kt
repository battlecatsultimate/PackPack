package mandarin.card.supporter

import common.pack.UserProfile
import common.util.Data
import common.util.unit.Trait
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.filter.*

class Product(val requiredFilter: Int, vararg filters: Filter) {
    companion object {
        val doge = Product(1, AnyFilter(5, "Any 5 Units"))

        val youcan = Product(2,
                TraitFilter(getTrait(Data.TRAIT_ANGEL), 3, "3 Anti-Angel Units"),
                TraitFilter(getTrait(Data.TRAIT_ALIEN), 3, "3 Anti-Alien Units"),
                ProcFilter("BREAK", 2, "2 Units with Barrier Breaker"),
                CustomFilter.withAntiWave(1, "1 Anti-Wave Unit"),
                ProcFilter("WAVE", 1, "1 Unit with Wave")
        )

        val gobble = Product(2,
                TraitFilter(getTrait(Data.TRAIT_FLOAT), 3, "3 Anti-Float Units"),
                CustomFilter.withOmni(1, "1 Omni Strike Unit"),
                ProcFilter("IMUPOIATK", 1, "1 Unit with Immunity to Toxin")
        )

        val lilDoge = Product(2,
            ProcFilter("IMUATK", 2, "2 Units with Invincibility"),
            TraitFilter(getTrait(Data.TRAIT_WHITE), 2, "2 Anti-White Units"),
            CustomFilter.withRangeNoLuga(2, "2 Units with Single Target")
        )

        val akuCyclone = Product(2,
            ProcFilter("SHIELDBREAK", 5, "5 Units with Shield Pierce"),
            TraitFilter(getTrait(Data.TRAIT_DEMON), 5, "5 Anti-Aku Units")
        )

        val relicBun = Product(2,
            TraitFilter(getTrait(Data.TRAIT_RELIC), 5, "5 Anti-Relic Units"),
            ProcFilter("IMUCURSE", 5, "5 Units with Immunity to Curse"),
            CustomFilter.withOmni(3, "3 Omni Strike Units"),
            ProcFilter("CURSE", 1, "1 Unit with Curse")
        )

        val exiel = Product(3,
            ProcFilter("IMUWARP", 5, "5 Units with Immunity to Warp"),
            ProcFilter("WEAK", 5, "5 Units with Weaken"),
            ProcFilter("IMUWEAK", 5, "5 Units with Immunity to Weaken"),
            TraitFilter(getTrait(Data.TRAIT_ANGEL), 7, "7 Anti-Angel Units"),
            CustomFilter.withOmni(3, "3 Omni Strike Units"),
            AbilityFilter(Data.AB_BAKILL, 3, "3 Units with Colossus Slayer")
        )

        val luza = Product(3,
            TraitFilter(getTrait(Data.TRAIT_RELIC), 9, "9 Anti-Relic Units"),
            ProcFilter("IMUCURSE", 9, "9 Units with Immunity to Curse"),
            CustomFilter.withOmni(5, "5 Omni Strike Units"),
            ProcFilter("STRONG", 5, "5 Units with Strengthen"),
            AbilityFilter(Data.AB_BAKILL, 5, "5 Units with Colossus Slayer"),
            ProcFilter("CURSE", 3, "3 Units with Curse")
        )

        val hermitCat = Product(3,
            ProcFilter("WAVE", 7, "7 Units with Wave"),
            CustomFilter.withAntiWave(7, "7 Anti-Wave Units"),
            TraitFilter(getTrait(Data.TRAIT_WHITE), 7, "7 Anti-Traitless Units")
        )

        val seasonal = Product(1,
            CustomFilter(3, "3 Seasonal Units") { c ->
                return@CustomFilter c.cardType == Card.CardType.SEASONAL && c.tier == CardData.Tier.UNCOMMON
            }
        )

        val ramiel = Product(1,
            CustomFilter(3, "3 Seasonal Units") { c ->
                return@CustomFilter c.cardType == Card.CardType.COLLABORATION && c.tier == CardData.Tier.UNCOMMON
            }
        )

        @Suppress("unused")
        val customEmoji = Product(1,
            CustomFilter(5, "5 Uberfest Exclusive Units") { c ->
                Banner.fromName("Uberfest Exclusive") in c.banner
            },
            CustomFilter(5, "5 Epicfest Exclusive Units") { c ->
                Banner.fromName("Epicfest Exclusive") in c.banner
            },
            CustomFilter(5, "5 Buster Exclusive Units") { c ->
                Banner.fromName("Buster Exclusive") in c.banner
            }
        )

        val customRole = Product(1,
            CustomFilter(10, "10 Legend Rare Units") { c ->
                return@CustomFilter c.tier == CardData.Tier.LEGEND
            }
        )

        private fun getTrait(trait: Byte) : Trait {
            return UserProfile.getBCData().traits[trait.toInt()]
        }
    }

    val possibleFilters = filters
}