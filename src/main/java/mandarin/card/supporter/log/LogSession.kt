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
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.abs
import kotlin.math.min

class LogSession {
    companion object {
        val globalFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")

        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss.S")

        var session = LogSession()
            private set

        init {
            globalFormat.timeZone = TimeZone.getTimeZone("UTC")
            format.timeZone = TimeZone.getTimeZone("UTC")
        }

        fun syncSession() {
            val currentTime = CardData.getUnixEpochTime()

            val logFolder = File("./data/cardLog")

            if (!logFolder.exists() && !logFolder.mkdirs())
                return

            val logFiles = logFolder.listFiles()

            if (logFiles == null) {
                session.saveSessionAsFile()

                return
            }

            if (logFiles.isEmpty()) {
                session.saveSessionAsFile()

                return
            }

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
            calendar.timeZone = TimeZone.getTimeZone("UTC")

            calendar.time = currentDate

            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            calendar.time = lastDate

            val lastMonth = calendar.get(Calendar.MONTH)
            val lastDay = calendar.get(Calendar.DAY_OF_MONTH)

            if (currentMonth > lastMonth || currentDay > lastDay) {
                session.saveSessionAsFile()

                val previousSession = session

                session = LogSession()

                StaticStore.logger.uploadLog("I/LogSession::syncSession\n\nArchived session for ${format.format(previousSession.createdTime)}\nStarting new session for ${format.format(session.createdTime)}")
            } else if (session.createdTime != selectedTime) {
                session = fromFile(selectedFile)

                StaticStore.logger.uploadLog("I/LogSession::syncSession\n\nBringing on-going session for ${format.format(session.createdTime)}")
            }

            session.saveSessionAsFile()
        }

        fun gatherPreviousSessions(from: Long, amount: Int) : ArrayList<LogSession> {
            val result = ArrayList<LogSession>()

            session.saveSessionAsFile()

            val folder = File("./data/cardLog")

            if (!folder.exists())
                return result

            val logLists = folder.listFiles() ?: return result

            val logFiles = logLists.filter { l -> l.lastModified() <= from }.toTypedArray()

            logFiles.sortBy { l -> l.lastModified() }
            logFiles.reverse()

            for (i in 0 until min(logFiles.size, if (amount <= 0) logFiles.size else amount)) {
                result.add(fromFile(logFiles[i]))
            }

            return result
        }

        private fun fromFile(file: File) : LogSession {
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

            if (obj.has("activeMembers")) {
                val array = obj.getAsJsonArray("activeMembers")

                session.activeMembers.addAll(array.map { e -> e.asLong })
            }

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
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val id = o.get("key").asLong
                        val cards = o.getAsJsonArray("val")

                        val cardMap = HashMap<Card, Long>()

                        cards.forEach MapParse@ { data ->
                            if (!data.isJsonObject)
                                return@MapParse

                            val dataObject = data.asJsonObject

                            if (dataObject.has("key") && dataObject.has("val")) {
                                val cardId = dataObject.get("key").asInt

                                val card = CardData.cards.find { c -> c.unitID == cardId } ?: return@MapParse

                                val amount = dataObject.get("val").asLong

                                cardMap[card] = amount
                            }
                        }

                        session.generatedCards[id] = cardMap
                    }
                }
            }

            if (obj.has("removedCards")) {
                val array = obj.getAsJsonArray("removedCards")

                array.forEach { e ->
                    val o = e.asJsonObject

                    if (o.has("key") && o.has("val")) {
                        val id = o.get("key").asLong
                        val cards = o.getAsJsonArray("val")

                        val cardMap = HashMap<Card, Long>()

                        cards.forEach MapParse@ { data ->
                            if (!data.isJsonObject)
                                return@MapParse

                            val dataObject = data.asJsonObject

                            if (dataObject.has("key") && dataObject.has("val")) {
                                val cardId = dataObject.get("key").asInt

                                val card = CardData.cards.find { c -> c.unitID == cardId } ?: return@MapParse

                                val amount = dataObject.get("val").asLong

                                cardMap[card] = amount
                            }
                        }

                        session.removedCards[id] = cardMap
                    }
                }
            }

            return session
        }
    }

    val createdTime: Long

    val activeMembers = HashSet<Long>()

    val tier2Cards = ArrayList<Card>()

    val catFoodPack = HashMap<Long, Long>()
    val catFoodCraft = HashMap<Long, Long>()
    val catFoodTrade = HashMap<Long, Long>()
    var catFoodTradeSum = 0L

    var craftFailures = 0L

    val generatedCards = HashMap<Long, HashMap<Card, Long>>()
    val removedCards = HashMap<Long, HashMap<Card, Long>>()

    val tradedCards = HashMap<Long, HashMap<Card, Long>>()

    constructor() {
        createdTime = CardData.getUnixEpochTime()
    }

    constructor(time: Long) {
        createdTime = time
    }

    fun logBuy(member: Long, usedCards: List<Card>) {
        val cardMap = generatedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            generatedCards[member] = newMap
            newMap
        }

        usedCards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logCraftFail(member: Long, usedCards: List<Card>, cf: Long) {
        val cardMap = removedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            removedCards[member] = newMap
            newMap
        }

        usedCards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        craftFailures++

        catFoodCraft[member] = (catFoodCraft[member] ?: 0) + cf

        activeMembers.add(member)
    }

    fun logCraftSuccess(member: Long, usedCards: List<Card>, card: Card) {
        val removedCardMap = removedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            removedCards[member] = newMap
            newMap
        }

        usedCards.forEach {
            removedCardMap[it] = (removedCardMap[it] ?: 0) + 1
        }

        tier2Cards.add(card)

        val generatedCardMap = generatedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            generatedCards[member] = newMap
            newMap
        }

        generatedCardMap[card] = (generatedCardMap[card] ?: 0) + 1

        activeMembers.add(member)
    }

    fun logManualRoll(member: Long, cards: List<Card>) {
        val cardMap = generatedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            generatedCards[member] = newMap
            newMap
        }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logModifyAdd(member: Long, cards: List<Card>) {
        val cardMap = generatedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            generatedCards[member] = newMap
            newMap
        }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logModifyRemove(member: Long, cards: List<Card>) {
        val cardMap = removedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            removedCards[member] = newMap
            newMap
        }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logRoll(member: Long, pack: CardData.Pack, cards: List<Card>) {
        val cf = if (pack == CardData.Pack.PREMIUM)
            0
        else
            pack.cost

        if (cf != 0)
            catFoodPack[member] = (catFoodPack[member] ?: 0) + cf

        val cardMap = generatedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            generatedCards[member] = newMap
            newMap
        }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logSalvage(member: Long, usedCards: List<Card>, cf: Long) {
        catFoodCraft[member] = (catFoodCraft[member] ?: 0) + cf

        val cardMap = removedCards[member] ?: run {
            val newMap = HashMap<Card, Long>()
            removedCards[member] = newMap
            newMap
        }

        usedCards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logTrade(session: TradingSession) {
        catFoodTrade[session.member[0]] = (catFoodTrade[session.member[0]] ?: 0) - session.suggestion[0].catFood
        catFoodTrade[session.member[1]] = (catFoodTrade[session.member[1]] ?: 0) + session.suggestion[0].catFood

        catFoodTrade[session.member[0]] = (catFoodTrade[session.member[0]] ?: 0) + session.suggestion[1].catFood
        catFoodTrade[session.member[1]] = (catFoodTrade[session.member[1]] ?: 0) - session.suggestion[1].catFood

        catFoodTradeSum += abs(session.suggestion[0].catFood - session.suggestion[1].catFood).toLong()

        activeMembers.addAll(session.member)

        val firstTraderMap = tradedCards[session.member[0]] ?: run {
            val newMap = HashMap<Card, Long>()

            tradedCards[session.member[0]] = newMap

            newMap
        }

        val secondTraderMap = tradedCards[session.member[1]] ?: run {
            val newMap = HashMap<Card, Long>()

            tradedCards[session.member[1]] = newMap

            newMap
        }

        session.suggestion[0].cards.forEach { card ->
            firstTraderMap[card] = (firstTraderMap[card] ?: 0) - 1
            secondTraderMap[card] = (secondTraderMap[card] ?: 0) + 1
        }

        session.suggestion[1].cards.forEach { card ->
            firstTraderMap[card] = (firstTraderMap[card] ?: 0) + 1
            secondTraderMap[card] = (secondTraderMap[card] ?: 0) - 1
        }
    }

    fun saveSessionAsFile() {
        val folder = File("./data/cardLog")

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/LogSession::generateLogFile - Failed to create folder : ${folder.absolutePath}")

            return
        }

        val name = format.format(createdTime)

        val targetFile = File(folder, "$name.txt")

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

        val memberArray = JsonArray()

        activeMembers.forEach { id ->
            memberArray.add(id)
        }

        obj.add("activeMembers", memberArray)

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

        generatedCards.forEach { (id, cardMap) ->
            val o = JsonObject()

            o.addProperty("key", id)

            val arr = JsonArray()

            cardMap.forEach { (card, amount) ->
                val co = JsonObject()

                co.addProperty("key", card.unitID)
                co.addProperty("val", amount)

                arr.add(co)
            }

            o.add("val", arr)

            generatedArray.add(o)
        }

        obj.add("generatedCards", generatedArray)

        val removedArray = JsonArray()

        removedCards.forEach { (id, cardMap) ->
            val o = JsonObject()

            o.addProperty("key", id)

            val arr = JsonArray()

            cardMap.forEach { (card, amount) ->
                val co = JsonObject()

                co.addProperty("key", card.unitID)
                co.addProperty("val", amount)

                arr.add(co)
            }

            o.add("val", arr)

            generatedArray.add(o)
        }

        obj.add("removedCards", removedArray)

        return obj
    }
}