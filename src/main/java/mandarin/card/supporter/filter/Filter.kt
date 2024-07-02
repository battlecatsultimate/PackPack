package mandarin.card.supporter.filter

import mandarin.card.supporter.card.Card
import mandarin.card.supporter.Inventory

abstract class Filter(val amount: Int, val name: String) {
    abstract fun filter(card: Card) : Boolean

    fun match(cards: List<Card>, inventory: Inventory) : Boolean {
        return cards.filter { c -> filter(c) }.sumOf { c -> inventory.cards[c] ?: 1 } >= amount
    }
}