package mandarin.card.supporter.filter

import common.pack.UserProfile
import common.util.Data.Proc.IMU
import common.util.Data.Proc.IMUAD
import mandarin.card.supporter.Card

class ProcFilter(private val proc: String, amount: Int, name: String) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        if (card.unitID < 0)
            return false

        val u = UserProfile.getBCData().units[card.unitID] ?: return false

        return u.forms.any { f ->
            val p = if (f.fid == 2 && f.du.pCoin != null) {
                f.du.pCoin.full.proc.get(proc)
            } else {
                f.du.proc.get(proc)
            }

            when (p) {
                is IMU -> p.mult == 100
                is IMUAD -> p.mult == 100
                else -> p.exists()
            }
        }
    }
}