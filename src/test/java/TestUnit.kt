import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBot.readCardData()

            println(CardData.getUnixEpochTime())
        }
    }
}