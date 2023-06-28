package mandarin.card.supporter.filter

import common.pack.UserProfile
import mandarin.card.supporter.Card

class ProcFilter(private val proc: String, amount: Int, name: String) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        val u = UserProfile.getBCData().units[card.unitID]

        return u.forms.any { f -> f.du.proc.get(proc).exists() }
    }
}