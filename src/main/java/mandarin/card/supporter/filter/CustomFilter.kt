package mandarin.card.supporter.filter

import mandarin.card.supporter.Card
import java.util.function.Function

class CustomFilter(amount: Int, name: String, private val function: Function<Card, Boolean>) : Filter(amount, name) {
    override fun filter(card: Card): Boolean {
        return function.apply(card)
    }
}