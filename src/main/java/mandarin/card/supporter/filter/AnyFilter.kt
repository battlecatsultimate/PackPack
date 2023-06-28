package mandarin.card.supporter.filter

import mandarin.card.supporter.Card

class AnyFilter(amount: Int, name: String) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        return true
    }
}