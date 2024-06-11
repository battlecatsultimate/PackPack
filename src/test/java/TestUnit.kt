import mandarin.card.supporter.PositiveMap
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val map = PositiveMap<String, Long>()

            map["hello"] = 1L

            println(map["hello"])
            println(map.containsKey("hello"))

            map["hello"] = (map["hello"] ?: 0L) - 2L

            println(map["hello"])
            println(map.containsKey("hello"))
        }
    }
}