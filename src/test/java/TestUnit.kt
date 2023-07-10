import mandarin.card.CardBot
import mandarin.card.supporter.CardData

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBot.readCardData()

            println(CardData.getUnixEpochTime())
        }
    }
}