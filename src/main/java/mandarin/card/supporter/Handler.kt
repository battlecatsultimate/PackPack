package mandarin.card.supporter

import java.util.Timer
import java.util.TimerTask

class Handler {
    private var timer = Timer()
    private var canceled = false

    fun post(executor: Runnable) {
        check()

        timer.schedule(object: TimerTask() {
            override fun run() {
                executor.run()
            }
        }, 0)
    }

    fun postDelayed(delay: Long, executor: Runnable) {
        check()

        timer.schedule(object: TimerTask() {
            override fun run() {
                executor.run()
            }
        }, delay)
    }

    fun cleanUp() {
        timer.cancel()
        timer.purge()

        canceled = true
    }

    private fun check() {
        if (canceled) {
            canceled = false
            timer = Timer()
        }
    }
}