package mandarin.card.supporter.filter

import common.pack.UserProfile
import common.util.Data
import common.util.Data.Proc.IMU
import common.util.Data.Proc.IMUAD
import common.util.Data.Proc.WAVEI
import mandarin.card.supporter.Card
import java.util.function.Function

class CustomFilter(amount: Int, name: String, private val function: Function<Card, Boolean>) : Filter(amount, name) {
    companion object {
        fun withOmni(amount: Int, name: String) : CustomFilter {
            return CustomFilter(amount, name) { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.isOmni } }
        }

        fun withLD(amount: Int, name: String) : CustomFilter {
            return CustomFilter(amount, name) { c -> UserProfile.getBCData().units[c.unitID].forms.any { f -> f.du.isLD } }
        }

        fun withRangeNoLuga(amount: Int, name: String) : CustomFilter {
            return CustomFilter(amount, name) { c ->
                val u = UserProfile.getBCData().units[c.unitID]

                if (c.unitID in BannerFilter.Banner.TheNekolugaFamily.getBannerData()) {
                    u.forms.any { f -> f.fid != 0 && !f.du.isRange }
                } else {
                    u.forms.any { f -> !f.du.isRange }
                }
            }
        }

        fun withAntiWave(amount: Int, name: String) : CustomFilter {
            return CustomFilter(amount, name) { c ->
                UserProfile.getBCData().units[c.unitID].forms.any { f ->
                    val du = if (f.fid == 2 && f.du.pCoin != null)
                        f.du.pCoin.full
                    else
                        f.du

                    if (du.abi and Data.AB_WAVES > 0)
                        return@CustomFilter true

                    when (val proc = du.proc.get("IMUWAVE")) {
                        is WAVEI -> proc.mult == 100
                        else -> false
                    }
                }
            }
        }
    }

    override fun filter(card: Card): Boolean {
        return function.apply(card)
    }
}