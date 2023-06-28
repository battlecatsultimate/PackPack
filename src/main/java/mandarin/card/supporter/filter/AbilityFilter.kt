package mandarin.card.supporter.filter

import common.pack.UserProfile
import mandarin.card.supporter.Card

class AbilityFilter(private val ability: Int, amount: Int, name: String) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        val unit = UserProfile.getBCData().units[card.unitID]

        return unit.forms.any { f -> f.du.abi and ability > 0 }
    }
}