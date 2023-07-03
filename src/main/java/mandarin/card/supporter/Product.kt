package mandarin.card.supporter

import common.pack.UserProfile
import common.util.Data
import common.util.unit.Trait
import mandarin.card.supporter.filter.*

class Product(val requiredFilter: Int, vararg filters: Filter) {
    companion object {
        val doge = Product(1, AnyFilter(5, "Any 5 Units"))

        val sirSeal = Product(1, TraitFilter(getTrait(Data.TRAIT_RED), 3, "3 Anti-Red Units"))

        val assassin = Product(1, TraitFilter(getTrait(Data.TRAIT_BLACK), 3, "3 Anti-Black Units"))

        val angelic = Product(1, TraitFilter(getTrait(Data.TRAIT_ANGEL), 3, "3 Anti-Angel Units"))

        val mooth = Product(1, TraitFilter(getTrait(Data.TRAIT_FLOAT), 3, "3 Anti-Float Units"))

        val smh = Product(1,
            TraitFilter(getTrait(Data.TRAIT_METAL), 3, "3 Anti-Metal Units"),
            ProcFilter("CRIT", 3, "3 Units with Critical"),
            ProcFilter("KB", 3, "3 Units with KB"),
            ProcFilter("IMUKB", 3, "3 Units with Immunity to KB")
        )

        val scissor = Product(2,
            TraitFilter(getTrait(Data.TRAIT_ALIEN), 5, "5 Anti-Alien Units"),
            ProcFilter("SLOW", 5, "5 Units with Slow"),
            ProcFilter("IMUSLOW", 5, "5 Units with Immunity to Slow"),
            CustomFilter(3, "3 LD Units") { c ->
                val u = UserProfile.getBCData().units[c.unitID]

                return@CustomFilter u.forms.any { f -> f.du.isLD }
            },
            ProcFilter("BSTHUNT", 1, "1 Unit with Behemoth Slayer")
        )

        val lilDoge = Product(2,
            ProcFilter("IMUATK", 2, "2 Units with Invincibility"),
            TraitFilter(getTrait(Data.TRAIT_WHITE), 2, "2 Anti-White Units"),
            CustomFilter(2, "2 Units with Single Target") { c ->
                val u = UserProfile.getBCData().units[c.unitID]

                if (c.unitID in BannerFilter.Banner.TheNekolugaFamily.getBannerData()) {
                    u.forms.any { f -> f.fid != 0 && !f.du.isRange }
                } else {
                    u.forms.any { f -> !f.du.isRange }
                }
            }
        )

        val daboo = Product(2,
            TraitFilter(getTrait(Data.TRAIT_ZOMBIE), 5, "5 Anti-Zombie Units"),
            AbilityFilter(Data.AB_ZKILL, 5, "5 Units with Zombie Killer"),
            ProcFilter("SLOW", 5, "5 Units with Slow"),
            CustomFilter(5, "5 Unis with Immunity to Slow/Freeze/KB/Weaken/Wave") { c ->
                val u = UserProfile.getBCData().units[c.unitID]

                arrayOf("IMUSLOW", "IMUSTOP", "IMUKB", "IMUWEAK", "IMUWAVE").any {
                    u.forms.any { f -> f.du.proc.get(it).exists() }
                }
            },
            CustomFilter(1, "1 Omni Strike Unit") { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.isOmni } }
        )

        val akuCyclone = Product(2,
            ProcFilter("SHIELDBREAK", 5, "5 Units with Shield Pierce"),
            TraitFilter(getTrait(Data.TRAIT_DEMON), 5, "5 Anti-Aku Units")
        )

        val relicBun = Product(2,
            TraitFilter(getTrait(Data.TRAIT_RELIC), 5, "5 Anti-Relic Units"),
            ProcFilter("IMUCURSE", 5, "5 Units with Immunity to Curse"),
            CustomFilter(3, "3 Omni Strike Units") { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.isOmni } },
            ProcFilter("CURSE", 1, "1 Unit with Curse")
        )

        val wildDoge = Product(2,
            TraitFilter(getTrait(Data.TRAIT_WHITE), 2, "2 Anti-Traitless Units"),
            ProcFilter("LETHAL", 2, "2 Units with Survive"),
            ProcFilter("VOLC", 2, "2 Units with Surge"),
            ProcFilter("IMUVOLC", 2, "2 Units with Immunity to Surge"),
            ProcFilter("BSTHUNT", 1, "1 Unit with Behemoth Slayer")
        )

        val exiel = Product(3,
            ProcFilter("IMUWARP", 7, "7 Units with Immunity to Warp"),
            ProcFilter("WEAK", 7, "7 Units with Weaken"),
            ProcFilter("IMUWEAK", 5, "5 Units with Immunity to Weaken"),
            TraitFilter(getTrait(Data.TRAIT_ANGEL), 9, "9 Anti-Angel Units"),
            AbilityFilter(Data.AB_CKILL, 3, "3 Units with Colossus Slayer")
        )

        val omens = Product(3,
            ProcFilter("STOP", 7, "7 Units with Freeze"),
            ProcFilter("IMUSTOP", 7, "7 Units with Immunity to Freeze"),
            TraitFilter(getTrait(Data.TRAIT_DEMON), 7, "7 Anti-Aku Units"),
            ProcFilter("IMUVOLC", 5, "5 Units with Immunity to Surge"),
            ProcFilter("VOLC", 3, "3 Units with Surge"),
            AbilityFilter(Data.AB_CKILL, 3, "3 Units with Colossus Slayer")
        )

        val luza = Product(3,
            TraitFilter(getTrait(Data.TRAIT_RELIC), 9, "9 Anti-Relic Units"),
            ProcFilter("IMUCURSE", 9, "9 Units with Immunity to Curse"),
            CustomFilter(5, "5 Omni Strike Units") { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.isOmni } },
            ProcFilter("STRONG", 5, "5 Units with Strengthen"),
            AbilityFilter(Data.AB_CKILL, 5, "5 Units with Colossus Slayer"),
            ProcFilter("CURSE", 3, "3 Units with Curse")
        )

        val hermitCat = Product(2,
            ProcFilter("WAVE", 11, "11 Units with Wave"),
            CustomFilter(11, "11 Units with Wave Blocker/Immunity to Wave") { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.abi and Data.AB_WAVES > 0 || f.du.proc.get("IMUWAVE").exists() } },
            TraitFilter(getTrait(Data.TRAIT_WHITE), 11, "11 Anti-Traitless Units")
        )

        val seasonal = Product(1,
            BannerFilter(BannerFilter.Banner.Seasonal, 3, "3 Sesonal Units")
        )

        val ramiel = Product(1,
            BannerFilter(BannerFilter.Banner.Collaboration, 3, "3 Collaboration Units")
        )

        val customEmoji = Product(1,
            BannerFilter(BannerFilter.Banner.UberfestExclusives, 5, "5 Uberfest Exclusive Units"),
            BannerFilter(BannerFilter.Banner.EpicfestExclusives, 5, "5 Epicfest Exclusive Units"),
            BannerFilter(BannerFilter.Banner.BusterExclusives, 5, "5 Buster Exclusive Units")
        )

        val customRole = Product(1,
            BannerFilter(BannerFilter.Banner.LegendRare, 5, "5 Legend Rare Units")
        )

        private fun getTrait(trait: Byte) : Trait {
            return UserProfile.getBCData().traits[trait.toInt()]
        }
    }

    val possibleFilters = filters
}