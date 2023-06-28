package mandarin.card.supporter.transaction

import mandarin.card.supporter.Handler
import mandarin.packpack.supporter.StaticStore
import java.time.Clock
import java.time.Instant
import java.util.*
import kotlin.collections.ArrayDeque
import kotlin.math.max

class TransactionGroup {
    companion object {
        val groupQueue = ArrayDeque<TransactionGroup>()

        private val queueHandler = Handler()

        fun activateHandler() {
            val delay = max(0, (TatsuHandler.nextRefreshTime - Instant.now(Clock.systemUTC()).epochSecond) * 1000 + 1000)

            val handle = object : Runnable {
                override fun run() {
                    val nextQueue = groupQueue.removeFirstOrNull() ?: return

                    nextQueue.queues.forEach { q -> q.handle() }

                    StaticStore.logger.uploadLog("Handled ${nextQueue.queues.size} queues\n\n${groupQueue.size} group(s) are waiting")

                    if (groupQueue.isNotEmpty()) {
                        val period = (TatsuHandler.nextRefreshTime - Instant.now(Clock.systemUTC()).epochSecond) * 1000 + 1000

                        queueHandler.postDelayed(period) {
                            run()
                        }
                    } else {
                        queueHandler.cleanUp()
                    }
                }
            }

            queueHandler.postDelayed(delay) {
                handle.run()
            }
        }

        fun queue(q: TransactionQueue) {
            //find possible queue
            var possibleQueue = groupQueue.find { g -> 60 - g.totalCost - q.cost >= 0 }

            if (possibleQueue == null) {
                possibleQueue = TransactionGroup()

                if (groupQueue.isEmpty()) {
                    activateHandler()
                }

                groupQueue.addLast(possibleQueue)
            }

            possibleQueue.queues.add(q)
        }
    }

    val queues = ArrayList<TransactionQueue>()
    val totalCost: Int get() {
        return queues.sumOf { q -> q.cost }
    }
}