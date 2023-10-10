package mandarin.card.supporter.log

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Member
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.abs

class LogSession {
    companion object {
        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.S")
        var session = LogSession()
            private set

        fun syncSession() {
            val currentTime = CardData.getUnixEpochTime()

            val logFolder = File("./data/cardLog")

            if (!logFolder.exists())
                return

            val logFiles = logFolder.listFiles() ?: return

            if (logFiles.isEmpty())
                return

            var selectedTime = 0L
            var selectedFile = File("./")

            for (log in logFiles) {
                if (!log.name.endsWith(".txt"))
                    continue

                val date = log.name.replace(".txt", "")

                val epoch = format.parse(date).time

                if (epoch > selectedTime) {
                    selectedTime = epoch
                    selectedFile = log
                }
            }

            val currentDate = Date(currentTime)
            val lastDate = Date(selectedTime)

            val calendar = Calendar.getInstance()

            calendar.time = currentDate

            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            calendar.time = lastDate

            val lastMonth = calendar.get(Calendar.MONTH)
            val lastDay = calendar.get(Calendar.DAY_OF_MONTH)

            if (currentMonth > lastMonth || currentDay > lastDay) {
                session.saveSessionAsFile()

                session = LogSession()
            } else if (session.createdTime != selectedTime) {
                session = fromFile(selectedFile)
            }

            session.saveSessionAsFile()
        }

        fun fromFile(file: File) : LogSession {
            val reader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))

            val element: JsonElement? = JsonParser.parseReader(reader)

            reader.close()

            if (element == null || !element.isJsonObject)
                return LogSession()

            val obj = element.asJsonObject

            if (!obj.has("createdTime")) {
                throw IOException("Invalid log session file : createdTime tag not found")
            }

            val session = LogSession(obj.get("createdTime").asLong)

            if (obj.has("tier2Cards")) {
                val array = obj.getAsJsonArray("tier2Cards")

                array.forEach { e ->
                    val id = e.asInt

                    val card = CardData.cards.find { c -> c.unitID == id }

                    if (card != null)
                        session.tier2Cards.add(card)
                }
            }

            if (obj.has("catFoodPack")) {
                val array = obj.getAsJsonArray("catFoodPack")

                array.forEach { e ->
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val id = o.get("key").asLong
                        val cf = o.get("val").asLong

                        session.catFoodPack[id] = cf
                    }
                }
            }

            if (obj.has("catFoodCraft")) {
                val array = obj.getAsJsonArray("catFoodCraft")

                array.forEach { e ->
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val id = o.get("key").asLong
                        val cf = o.get("val").asLong

                        session.catFoodCraft[id] = cf
                    }
                }
            }

            if (obj.has("catFoodTrade")) {
                val array = obj.getAsJsonArray("catFoodTrade")

                array.forEach { e ->
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val id = o.get("key").asLong
                        val cf = o.get("val").asLong

                        session.catFoodTrade[id] = cf
                    }
                }
            }

            if (obj.has("catFoodTradeSum")) {
                session.catFoodTradeSum = obj.get("catFoodTradeSum").asLong
            }

            if (obj.has("craftFailures")) {
                session.craftFailures = obj.get("craftFailures").asLong
            }

            if (obj.has("generatedCards")) {
                val array = obj.getAsJsonArray("generatedCards")

                array.forEach { e ->
                    val id = e.asInt

                    val card = CardData.cards.find { c -> c.unitID == id }

                    if (card != null) {
                        session.generatedCards.add(card)
                    }
                }
            }

            if (obj.has("removedCards")) {
                val array = obj.getAsJsonArray("removedCards")

                array.forEach { e ->
                    val id = e.asInt

                    val card = CardData.cards.find { c -> c.unitID == id }

                    if (card != null) {
                        session.removedCards.add(card)
                    }
                }
            }

            return session
        }
    }

    private val createdTime: Long

    private val tier2Cards = ArrayList<Card>()

    private val catFoodPack = HashMap<Long, Long>()
    private val catFoodCraft = HashMap<Long, Long>()
    private val catFoodTrade = HashMap<Long, Long>()
    private var catFoodTradeSum = 0L

    private var craftFailures = 0L

    private val generatedCards = ArrayList<Card>()
    private val removedCards = ArrayList<Card>()

    constructor() {
        createdTime = CardData.getUnixEpochTime()
    }

    constructor(time: Long) {
        createdTime = time
    }

    fun logBuy(usedCards: List<Card>) {
        removedCards.addAll(usedCards)
    }

    fun logCraftFail(m: Long, usedCards: List<Card>, cf: Long) {
        removedCards.addAll(usedCards)

        craftFailures++

        catFoodCraft[m] = (catFoodCraft[m] ?: 0) + cf
    }

    fun logCraftSuccess(usedCards: List<Card>, card: Card) {
        removedCards.addAll(usedCards)

        tier2Cards.add(card)
    }

    fun logManualRoll(result: List<Card>) {
        generatedCards.addAll(result)
    }

    fun logModifyAdd(cards: List<Card>) {
        generatedCards.addAll(cards)
    }

    fun logModifyRemove(cards: List<Card>) {
        removedCards.addAll(cards)
    }

    fun logRoll(m: Member, pack: CardData.Pack, result: List<Card>) {
        val cf = if (pack == CardData.Pack.PREMIUM)
            0
        else
            pack.cost

        if (cf != 0)
            catFoodPack[m.idLong] = (catFoodPack[m.idLong] ?: 0) + cf

        generatedCards.addAll(result)
    }

    fun logSalvage(m: Long, cards: List<Card>, cf: Long) {
        catFoodCraft[m] = (catFoodCraft[m] ?: 0) + cf

        removedCards.addAll(cards)
    }

    fun logTrade(session: TradingSession) {
        catFoodTrade[session.member[0]] = (catFoodTrade[session.member[0]] ?: 0) - session.suggestion[0].catFood
        catFoodTrade[session.member[1]] = (catFoodTrade[session.member[1]] ?: 0) + session.suggestion[0].catFood

        catFoodTrade[session.member[0]] = (catFoodTrade[session.member[0]] ?: 0) + session.suggestion[1].catFood
        catFoodTrade[session.member[1]] = (catFoodTrade[session.member[1]] ?: 0) - session.suggestion[1].catFood

        catFoodTradeSum += abs(session.suggestion[0].catFood - session.suggestion[1].catFood).toLong()
    }

    fun saveSessionAsFile() {
        val folder = File("./data/cardLog")

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/LogSession::generateLogFile - Failed to create folder : ${folder.absolutePath}")

            return
        }

        val name = format.format(createdTime)

        val targetFile = File("$name.txt")

        if (!targetFile.exists() && !targetFile.createNewFile()) {
            StaticStore.logger.uploadLog("W/LogSession::generateLogFile - Failed to create log file : ${targetFile.absolutePath}")

            return
        }

        val obj = asJsonObject()

        val mapper = ObjectMapper()

        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

        val json = obj.toString()

        val tree = mapper.readTree(json)

        val writer = FileWriter(targetFile)

        writer.append(mapper.writeValueAsString(tree))
        writer.close()
    }

    private fun asJsonObject() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("createdTime", createdTime)

        val t2Array = JsonArray()

        tier2Cards.forEach { c -> t2Array.add(c.unitID) }

        obj.add("tier2Cards", t2Array)

        val packArray = JsonArray()

        catFoodPack.forEach { (member, cf) ->
            val o = JsonObject()

            o.addProperty("key", member)
            o.addProperty("val", cf)

            packArray.add(o)
        }

        obj.add("catFoodPack", packArray)

        val craftArray = JsonArray()

        catFoodCraft.forEach { (member, cf) ->
            val o = JsonObject()

            o.addProperty("key", member)
            o.addProperty("val", cf)

            craftArray.add(o)
        }

        obj.add("catFoodCraft", craftArray)

        val tradeArray = JsonArray()

        catFoodTrade.forEach { (member, cf) ->
            val o = JsonObject()

            o.addProperty("key", member)
            o.addProperty("val", cf)

            tradeArray.add(o)
        }

        obj.add("catFoodTrade", tradeArray)

        obj.addProperty("catFoodTradeSum", catFoodTradeSum)

        obj.addProperty("craftFailures", craftFailures)

        val generatedArray = JsonArray()

        generatedCards.forEach { c -> generatedArray.add(c.unitID) }

        obj.add("generatedCards", generatedArray)

        val removedArray = JsonArray()

        removedCards.forEach { c -> removedArray.add(c.unitID) }

        obj.add("removedCards", removedArray)

        return obj
    }
}