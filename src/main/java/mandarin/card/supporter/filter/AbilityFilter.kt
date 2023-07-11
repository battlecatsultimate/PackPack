package mandarin.card.supporter.filter

import common.pack.UserProfile
import mandarin.card.supporter.Card

class AbilityFilter(private val ability: Int, amount: Int, name: String) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        val unit = UserProfile.getBCData().units[card.unitID]

        return unit.forms.any { f ->
            if (f.fid == 2 && f.du.pCoin != null) {
                f.du.pCoin.full.abi and ability > 0
            } else {
                f.du.abi and ability > 0
            }
        }
    }
}