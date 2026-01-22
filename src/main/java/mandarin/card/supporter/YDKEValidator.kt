package mandarin.card.supporter

import mandarin.packpack.supporter.StaticStore
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
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

    const val BCTCG = 210660000L

    val normalWhiteList = HashMap<Long, Int>()
    val tournamentWhiteList = HashMap<Long, Int>()
    val BCEWhiteList = HashMap<Long, Int>()

    val aliasData = HashMap<Long, ArrayList<Long>>()
    val cardName = HashMap<Long, String>()

    fun loadWhiteListData() {
        normalWhiteList.clear()
        tournamentWhiteList.clear()
        BCEWhiteList.clear()

        val normal = File("./data/bctcg/Normal.lflist.conf")

        if (normal.exists()) {
            BufferedReader(FileReader(normal)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("\\d+ \\d+( .+)?"))) {
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

        val tournament = File("./data/bctcg/Tournament.lflist.conf")

        if (tournament.exists()) {
            BufferedReader(FileReader(tournament)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("\\d+ \\d+( .+)?"))) {
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

        val bce = File("./data/bctcg/BCE.lflist.conf")

        if (bce.exists()) {
            BufferedReader(FileReader(bce)).use { reader ->
                var line = ""

                while (reader.readLine()?.also { line = it } != null) {
                    if (line.matches(Regex("\\d+ \\d+( .+)?"))) {
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

    fun loadCDBData() {
        aliasData.clear()
        cardName.clear()

        val cdbList = arrayListOf("cards.cdb", "BCTC.cdb")

        cdbList.forEach { cdb ->
            val file = File("./data/bctcg/$cdb")

            if (!file.exists())
                return

            Class.forName("org.sqlite.JDBC")

            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
                connection.createStatement().use { statement ->
                    val dataResult = statement.executeQuery("select * from datas")

                    while (dataResult.next()) {
                        val id = dataResult.getLong("id")
                        val alias = dataResult.getLong("alias")

                        if (alias == 0L)
                            continue

                        val list = aliasData[alias] ?: arrayListOf()

                        if (id !in list)
                            list.add(id)

                        aliasData[alias] = list
                    }

                    val textResult = statement.executeQuery("select * from texts")

                    while (textResult.next()) {
                        val id = textResult.getLong("id")
                        val name = textResult.getString("name")

                        if (name == null || name.isBlank())
                            continue

                        cardName[id] = name
                    }
                }
            }
        }
    }

    fun sanitize(inventory: Inventory, link: String, whiteList: WhiteList) : Pair<String, ArrayList<String>> {
        val data = toData(link)
        val reasons = ArrayList<String>()

        val cards = HashMap<Long, Int>()

        inventory.cards.filterKeys { card -> card.tier != CardData.Tier.SPECIAL }.forEach { (card, amount) ->
            val id = BCTCG + card.id

            cards[id] = (cards[id] ?: 0) + amount
        }

        inventory.favorites.filterKeys { card -> card.tier != CardData.Tier.SPECIAL }.forEach { (card, amount) ->
            val id = BCTCG + card.id

            cards[id] = (cards[id] ?: 0) + amount
        }

        inventory.validationCards.forEach { (card, pair) ->
            val id = BCTCG + card.id

            cards[id] = (cards[id] ?: 0) + pair.second
        }

        data.forEachIndexed { index, segment ->
            val deckName = when(index) {
                MAIN -> "Main"
                EXTRA -> "Extra"
                else -> "Side"
            }

            segment.removeIf { value ->
                if (value >= BCTCG && value - BCTCG < 1000 && !cards.containsKey(value)) {
                    val name = if (cardName.containsKey(value)) {
                        "${cardName[value]} [$value]"
                    } else {
                        "Card $value"
                    }

                    val reason = "$deckName : $name doesn't exist in this user's inventory. Removing cards"

                    if (reason !in reasons) {
                        reasons.add(reason)
                    }

                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }

            val segmentMap = HashMap<Long, Int>()

            segment.filter { value -> value >= BCTCG && value - BCTCG < 1000 }.forEach { value ->
                segmentMap[value] = (segmentMap[value] ?: 0) + 1
            }

            segmentMap.forEach { (value, amount) ->
                val trueAmount = cards[value] ?: 0

                if (trueAmount < amount) {
                    val name = if (cardName.containsKey(value)) {
                        "${cardName[value]} [$value]"
                    } else {
                        "Card $value"
                    }

                    reasons.add("$deckName : For $name, deck contains $amount card${if (amount >= 2) "s" else ""} while inventory contains $trueAmount card${if (trueAmount > 2) "s" else ""}. Limiting to $trueAmount")

                    repeat(amount - trueAmount) {
                        segment.remove(value)
                    }
                }
            }

            val whiteListData = when(whiteList) {
                WhiteList.NORMAL -> normalWhiteList
                WhiteList.TOURNAMENT -> tournamentWhiteList
                WhiteList.BCE -> BCEWhiteList
            }

            segment.removeIf { value ->
                if (!whiteListData.containsKey(value) && !aliasData.values.any { list -> value in list }) {
                    val name = if (cardName.containsKey(value)) {
                        "${cardName[value]} [$value]"
                    } else {
                        "Card $value"
                    }

                    val reason = "$deckName : White list data doesn't contain $name. Removing cards"

                    if (reason !in reasons)
                        reasons.add(reason)

                    return@removeIf true
                } else {
                    return@removeIf false
                }
            }

            whiteListData.forEach { (id, limit) ->
                val cardMap = HashMap<Long, Int>()
                val alias = aliasData[id]

                cardMap[id] = segment.count { value -> value == id }

                alias?.forEach { a ->
                    cardMap[a] = segment.count { value -> value == a }
                }

                val group = ArrayList(cardMap.keys)

                val totalAmount = cardMap.values.sumOf { v -> v }

                if (totalAmount > limit) {
                    val removedMap = HashMap<Long, Int>()

                    repeat(totalAmount - limit) {
                        val lowestID = cardMap.keys.filter { v -> v in segment }.minOf { v -> v }

                        segment.remove(lowestID)
                        cardMap[lowestID] = (cardMap[lowestID] ?: 0) - 1
                        removedMap[lowestID] = (removedMap[lowestID] ?: 0) + 1

                        if ((cardMap[lowestID] ?: 0) == 0) {
                            cardMap.remove(lowestID)
                        }
                    }

                    val text = if (group.size == 1) {
                        val name = if (cardName.containsKey(group.first())) {
                            "${cardName[group.first()]} [${group.first()}]"
                        } else {
                            "Card ${group.first()}"
                        }

                        "$deckName : Deck contains $totalAmount card${if (totalAmount >= 2) "s" else ""} while only $limit card${if (limit >= 2) "s are" else " is"} allowed for $name. Limiting to $limit"
                    } else {
                        val name = group.map { value ->
                            return@map if (cardName.containsKey(value)) {
                                "${cardName[value]} [$value]"
                            } else {
                                "Card $value"
                            }
                        }.joinToString(",", "[", "]")

                        "$deckName : Deck contains $totalAmount card${if (totalAmount >= 2) "s" else ""} while only $limit card${if (limit >= 2) "s are" else " is"} allowed for group of $name. Limiting to $limit"
                    }

                    reasons.add(text)
                    removedMap.forEach { (value, amount) ->
                        reasons.add("  - Removed $amount card $value")
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