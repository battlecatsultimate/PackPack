package mandarin.card.supporter.transaction

class TransactionQueue(val cost: Int, private val runnable: Runnable) {
    fun handle() {
        runnable.run()
    }
}