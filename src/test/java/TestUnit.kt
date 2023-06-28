import mandarin.card.CardBot
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionGroup

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            CardBot.readCardData()

            TransactionGroup.activateHandler()

            TatsuHandler.API = args[0]

            repeat(60) {
                TatsuHandler.transferPoints(null, 642008685199228940L, 460409259021172781L, 460409259021172781L, 120000)
            }
        }
    }
}