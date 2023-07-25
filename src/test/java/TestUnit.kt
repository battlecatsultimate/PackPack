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

                when(t.name) {
                    "Tier 1" -> println("---------- Tier 1 ----------")
                    "Tier 2" -> println("---------- Tier 2 ----------")
                    "Tier 3" -> println("---------- Tier 3 ----------")
                    "Tier 4" -> println("---------- Tier 4 ----------")
                    else -> throw IllegalStateException("Invalid tier type ${t.name} found")
                }

                for(card in cards) {
                    val nameData = card.name.replace(".png", "").split(Regex("-"), 2)

                    println(nameData[0] + " - " + nameData[1])
                }
            }
        }
    }
}