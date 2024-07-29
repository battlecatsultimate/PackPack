package mandarin.card.commands

import common.CommonStatic
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.bc.DataToString
import mandarin.packpack.supporter.server.CommandLoader
import kotlin.math.round

class Memory : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val f = Runtime.getRuntime().freeMemory()
        val t = Runtime.getRuntime().totalMemory()
        val m = Runtime.getRuntime().maxMemory()

        val percentage = 100.0 * (t - f) / m

        replyToMessageSafely(loader.channel, "Memory Used : ${t - f shr 20} MB / ${m shr 20} MB, ${DataToString.df.format(percentage)}%", loader.message) { a -> a }
    }
}