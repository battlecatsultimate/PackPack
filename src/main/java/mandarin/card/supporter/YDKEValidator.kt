package mandarin.card.supporter

import mandarin.card.supporter.CardData.Tier
import mandarin.packpack.supporter.StaticStore
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.util.*

object YDKEValidator {
    enum class WhiteList {
        NORMAL,
        TOURNAMENT,
        BCE
    }

    const val MAIN = 0
    const val EXTRA = 1
    const val SIDE = 2

    const val BCTCG = 210660000

    val normalWhiteList = HashMap<Long, Int>()
    val tournamentWhiteList = HashMap<Long, Int>()
    val BCEWhiteList = HashMap<Long, Int>()

    fun loadWhiteListData() {
        normalWhiteList.clear()
        tournamentWhiteList.clear()
        BCEWhiteList.clear()

        val normal = File("./data/Normal.lflist.conf")

        if (normal.exists()) {
            BufferedReader(FileReader(normal)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("^\\d+ \\d+\\D*"))) {
                        val data = line.split(" ")

                        if (data.size < 2)
                            continue

                        if (!StaticStore.isNumeric(data[0]) || !StaticStore.isNumeric(data[1]))
                            continue

                        normalWhiteList[StaticStore.safeParseLong(data[0])] = StaticStore.safeParseInt(data[1])
                    }
                }
            }
        }

        val tournament = File("./data/Tournament.lflist.conf")

        if (tournament.exists()) {
            BufferedReader(FileReader(tournament)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("^\\d+ \\d+\\D*"))) {
                        val data = line.split(" ")

                        if (data.size < 2)
                            continue

                        if (!StaticStore.isNumeric(data[0]) || !StaticStore.isNumeric(data[1]))
                            continue

                        tournamentWhiteList[StaticStore.safeParseLong(data[0])] = StaticStore.safeParseInt(data[1])
                    }
                }
            }
        }

        val bce = File("./data/BCE.lflist.conf")

        if (bce.exists()) {
            BufferedReader(FileReader(bce)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("^\\d+ \\d+\\D*"))) {
                        val data = line.split(" ")

                        if (data.size < 2)
                            continue

                        if (!StaticStore.isNumeric(data[0]) || !StaticStore.isNumeric(data[1]))
                            continue

                        BCEWhiteList[StaticStore.safeParseLong(data[0])] = StaticStore.safeParseInt(data[1])
                    }
                }
            }
        }
    }

    fun sanitize(inventory: Inventory, link: String, whiteList: WhiteList) : Pair<String, ArrayList<String>> {
        val data = toData(link)
        val reasons = ArrayList<String>()

        val cards = inventory.cards.filterKeys { card -> card.tier != Tier.SPECIAL }.keys.map { card -> (BCTCG + card.id).toLong() }

        data.forEach { segment ->
            segment.removeIf { value ->
                if (value - BCTCG < 1000 && value !in cards) {
                    val reason = "Card $value doesn't exist in this user's inventory"

                    if (reason !in reasons) {
                        reasons.add(reason)
                    }

                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }

            val cardMap = HashMap<Long, Int>()

            segment.forEach { value ->
                cardMap[value] = (cardMap[value] ?: 0) + 1
            }

            val whiteListData = when(whiteList) {
                WhiteList.NORMAL -> normalWhiteList
                WhiteList.TOURNAMENT -> tournamentWhiteList
                WhiteList.BCE -> BCEWhiteList
            }

            cardMap.forEach { (value, amount) ->
                val limit = whiteListData[value] ?: return@forEach

                if (amount > limit) {
                    reasons.add("Deck owns $amount card${if (amount > 2) "s" else ""} while only $limit card${if (limit > 2) "s" else ""} allowed. Limiting...")

                    repeat(amount - limit) {
                        segment.remove(value)
                    }
                }
            }
        }

        return Pair(toLink(data), reasons)
    }

    fun toData(link: String) : ArrayList<ArrayList<Long>> {
        val result = ArrayList<ArrayList<Long>>()

        val decoder = Base64.getDecoder()

        val filtered = link.replace("ydke://", "")

        val rawData = filtered.split("!")

        for (i in 0 until 3) {
            if (i >= rawData.size) {
                result.add(ArrayList())
            } else {
                val byteData = decoder.decode(rawData[i])

                val filteredData = ArrayList<ArrayList<Byte>>()

                var bytes = ArrayList<Byte>()

                for (i in 0 until byteData.size) {
                    bytes.add(byteData[byteData.size - 1 - i])

                    if (i % 4 == 3) {
                        filteredData.add(bytes)

                        bytes = ArrayList()
                    }
                }

                if (bytes.isNotEmpty()) {
                    filteredData.add(bytes)
                }

                val parsed = ArrayList<Long>()

                filteredData.forEach { filteredBytes ->
                    var value = 0L

                    filteredBytes.forEachIndexed { index, b ->
                        value += b.toUByte().toLong() shl ((filteredBytes.size - 1 - index) * 8)
                    }

                    parsed.add(value)
                }

                parsed.reverse()

                result.add(parsed)
            }
        }

        return result
    }

    fun toLink(data: ArrayList<ArrayList<Long>>) : String {
        val encoder = Base64.getEncoder()

        val builder = StringBuilder("ydke://")

        data.forEachIndexed { index, segment ->
            val byteData = ArrayList<Byte>()

            segment.forEach { value ->
                byteData.add((value and 0xFF).toByte())
                byteData.add(((value and 0xFF00) shr 8).toByte())
                byteData.add(((value and 0xFF0000) shr 16).toByte())
                byteData.add(((value and 0xFF000000) shr 24).toByte())
            }

            val encoded = encoder.encode(byteData.toByteArray()).toString(StandardCharsets.UTF_8)

            builder.append(encoded)

            if (index < SIDE) {
                builder.append("!")
            }
        }

        return builder.toString()
    }
}