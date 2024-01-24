import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val executor = Timer()

            var thread: Thread? = null

            executor.schedule(object : TimerTask() {
                override fun run() {
                    println("!?")

                    if (thread == null) {
                        thread = Thread.currentThread()
                    } else {
                        println(Thread.currentThread() === thread)
                        println(Thread.currentThread().name)
                        println(thread?.name)
                        println("---")
                    }
                }
            }, 0L, 1000L)
        }
    }
}