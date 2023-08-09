import common.util.Data
import java.io.File

class TestUnit {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val cardFolder = File("./data/cards")

            if (!cardFolder.exists())
                return

            val tiers = cardFolder.listFiles() ?: return

            for(t in tiers) {
                val cards = t.listFiles() ?: continue

                cards.sortBy { it.name.split("-")[0].toInt() }

                for(card in cards) {
                    val nameData = card.name.replace(".png", "").split(Regex("-"), 2)

                    println("210660${nameData[0]} 3 --${nameData[1]}")
                }
            }
        }
    }
}