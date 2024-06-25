package mandarin.card.supporter

import common.pack.UserProfile
import common.util.Data
import common.util.Data.Proc.IMU
import common.util.Data.Proc.IMUAD
import common.util.Data.Proc.WAVEI
import common.util.unit.Trait
import mandarin.card.supporter.filter.*

class Product(val requiredFilter: Int, vararg filters: Filter) {
    companion object {
        val doge = Product(1, AnyFilter(5, "Any 5 Units"))

        val twocan = Product(2,
                TraitFilter(getTrait(Data.TRAIT_RED), 3, "3 Anti-Red Units"),
                TraitFilter(getTrait(Data.TRAIT_BLACK), 3, "3 Anti-Black Units"),
                CustomFilter.withAntiWave(1, "1 Anti-Wave Unit"),
                ProcFilter("WAVE", 1, "1 Unit with Wave")
        )

        val ahirujo = Product(2,
                TraitFilter(getTrait(Data.TRAIT_RED), 5, "5 Anti-Red Units"),
                CustomFilter.withOmni(2, "2 Omni Strike Units"),
                ProcFilter("SATK", 1, "1 Unit with Savage Blow"),
                ProcFilter("BSTHUNT", 1, "1 Unit with Behemoth Slayer")
        )

        val bakoo = Product(2,
                TraitFilter(getTrait(Data.TRAIT_BLACK), 5, "5 Anti-Black Units"),
                ProcFilter("STOP", 3, "3 Units with Freeze"),
                ProcFilter("IMUSTOP", 3, "3 Units with Immunity to Freeze"),
                CustomFilter.withLD(1, "1 LD Unit")
        )

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

        val smh = Product(2,
            TraitFilter(getTrait(Data.TRAIT_METAL), 3, "3 Anti-Metal Units"),
            ProcFilter("CRIT", 1, "1 Unit with Critical"),
            ProcFilter("KB", 1, "1 Unit with KB"),
            ProcFilter("IMUKB", 1, "1 Unit with Immunity to KB")
        )

        val scissor = Product(2,
            TraitFilter(getTrait(Data.TRAIT_ALIEN), 5, "5 Anti-Alien Units"),
            ProcFilter("SLOW", 3, "3 Units with Slow"),
            ProcFilter("IMUSLOW", 3, "3 Units with Immunity to Slow"),
            CustomFilter.withLD(1, "1 LD Unit"),
            ProcFilter("BSTHUNT", 1, "1 Unit with Behemoth Slayer")
        )

        val lilDoge = Product(2,
            ProcFilter("IMUATK", 2, "2 Units with Invincibility"),
            TraitFilter(getTrait(Data.TRAIT_WHITE), 2, "2 Anti-White Units"),
            CustomFilter.withRangeNoLuga(2, "2 Units with Single Target")
        )

        val daboo = Product(2,
            TraitFilter(getTrait(Data.TRAIT_ZOMBIE), 5, "5 Anti-Zombie Units"),
            AbilityFilter(Data.AB_ZKILL, 5, "5 Units with Zombie Killer"),
            ProcFilter("SLOW", 3, "3 Units with Slow"),
            CustomFilter(3, "3 Unis with Immunity to Slow/Freeze/KB/Weaken/Wave") { c ->
                if (c.unitID < 0)
                    return@CustomFilter false

                val u = UserProfile.getBCData().units[c.unitID] ?: return@CustomFilter false

                arrayOf("IMUSLOW", "IMUSTOP", "IMUKB", "IMUWEAK", "IMUWAVE").any {
                    u.forms.any { f ->
                        when (val proc = f.du.proc.get(it)) {
                            is IMU -> proc.mult == 100
                            is IMUAD -> proc.mult == 100
                            is WAVEI -> proc.mult == 100
                            else -> false
                        }
                    }
                }
            },
            CustomFilter.withOmni(2, "2 Omni Strike Units")
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

        val wildDoge = Product(2,
            TraitFilter(getTrait(Data.TRAIT_WHITE), 2, "2 Anti-Traitless Units"),
            ProcFilter("LETHAL", 2, "2 Units with Survive"),
            ProcFilter("BSTHUNT", 1, "1 Unit with Behemoth Slayer")
        )

        val exiel = Product(3,
            ProcFilter("IMUWARP", 5, "5 Units with Immunity to Warp"),
            ProcFilter("WEAK", 5, "5 Units with Weaken"),
            ProcFilter("IMUWEAK", 5, "5 Units with Immunity to Weaken"),
            TraitFilter(getTrait(Data.TRAIT_ANGEL), 7, "7 Anti-Angel Units"),
            CustomFilter.withOmni(3, "3 Omni Strike Units"),
            AbilityFilter(Data.AB_BAKILL, 3, "3 Units with Colossus Slayer")
        )

        val omens = Product(3,
            ProcFilter("STOP", 5, "5 Units with Freeze"),
            ProcFilter("IMUSTOP", 5, "5 Units with Immunity to Freeze"),
            TraitFilter(getTrait(Data.TRAIT_DEMON), 7, "7 Anti-Aku Units"),
            ProcFilter("IMUVOLC", 5, "5 Units with Immunity to Surge"),
            ProcFilter("VOLC", 3, "3 Units with Surge"),
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
            BannerFilter(BannerFilter.Banner.Seasonal, 3, "3 Seasonal Units")
        )

        val ramiel = Product(1,
            BannerFilter(BannerFilter.Banner.Collaboration, 3, "3 Collaboration Units")
        )

        @Suppress("unused")
        val customEmoji = Product(1,
            BannerFilter(BannerFilter.Banner.UberfestExclusives, 5, "5 Uberfest Exclusive Units"),
            BannerFilter(BannerFilter.Banner.EpicfestExclusives, 5, "5 Epicfest Exclusive Units"),
            BannerFilter(BannerFilter.Banner.BusterExclusives, 5, "5 Buster Exclusive Units")
        )

        val customRole = Product(1,
            BannerFilter(BannerFilter.Banner.LegendRare, 10, "10 Legend Rare Units")
        )

        private fun getTrait(trait: Byte) : Trait {
            return UserProfile.getBCData().traits[trait.toInt()]
        }
    }

    val possibleFilters = filters
}