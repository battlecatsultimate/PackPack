package mandarin.card.supporter.log

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.TradingSession
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.slot.SlotEntryFee
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

            val logFolder = File(if (CardBot.test) "./data/testCardLog" else "./data/cardLog")

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

            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            calendar.time = lastDate

            val lastYear = calendar.get(Calendar.YEAR)
            val lastMonth = calendar.get(Calendar.MONTH)
            val lastDay = calendar.get(Calendar.DAY_OF_MONTH)

            if (currentYear > lastYear || currentMonth > lastMonth || currentDay > lastDay) {
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

            val folder = File(if (CardBot.test) "./data/testCardLog" else "./data/cardLog")

            if (!folder.exists())
                return result

            val logLists = folder.listFiles() ?: return result

            val logFiles = logLists.filter { l -> l.lastModified() <= from }.toTypedArray()

            logFiles.sortBy { l -> l.lastModified() }
            logFiles.reverse()

            for (i in 0 until min(logFiles.size, if (amount <= 0) logFiles.size else amount)) {
                result.add(fromFile(logFiles[i]))
            }

            if (!result.any { s -> s.createdTime == session.createdTime } && from >= session.createdTime) {
                result.add(0, session)
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

            loadUserAmount(obj, "catFoodPack", session.catFoodPack)

            loadUserCardMap(obj, "generatedCards", session.generatedCards)

            loadUserCardMap(obj, "removedCardsSalvage", session.removedCardsSalvage)
            loadUserCardMap(obj, "removedCardsManager", session.removedCardsManager)

            loadUserCardMap(obj, "removedCards", session.removedCards)

            loadUserAmount(obj, "shardTotal", session.shardTotal)

            loadUserAmount(obj, "shardSalvageT1", session.shardSalvageT1)
            loadUserAmount(obj, "shardSalvageT2Regular", session.shardSalvageT2Regular)
            loadUserAmount(obj, "shardSalvageT2Seasonal", session.shardSalvageT2Seasonal)
            loadUserAmount(obj, "shardSalvageT2Collaboration", session.shardSalvageT2Collaboration)
            loadUserAmount(obj, "shardSalvageT3", session.shardSalvageT3)

            loadUserCardMap(obj, "shardSalvageCardT1", session.shardSalvageCardT1)
            loadUserCardMap(obj, "shardSalvageCardT2Regular", session.shardSalvageCardT2Regular)
            loadUserCardMap(obj, "shardSalvageCardT2Seasonal", session.shardSalvageCardT2Seasonal)
            loadUserCardMap(obj, "shardSalvageCardT2Collaboration", session.shardSalvageCardT2Collaboration)
            loadUserCardMap(obj, "shardSalvageCardT3", session.shardSalvageCardT3)

            loadUserAmount(obj, "shardCraftT2Regular", session.shardCraftT2Regular)
            loadUserAmount(obj, "shardCraftT2Seasonal", session.shardCraftT2Seasonal)
            loadUserAmount(obj, "shardCraftT2Collaboration", session.shardCraftT2Collaboration)
            loadUserAmount(obj, "shardCraftT3", session.shardCraftT3)
            loadUserAmount(obj, "shardCraftT4", session.shardCraftT4)

            loadUserCardMap(obj, "shardCraftCardT2Regular", session.shardCraftCardT2Regular)
            loadUserCardMap(obj, "shardCraftCardT2Seasonal", session.shardCraftCardT2Seasonal)
            loadUserCardMap(obj, "shardCraftCardT2Collaboration", session.shardCraftCardT2Collaboration)
            loadUserCardMap(obj, "shardCraftCardT3", session.shardCraftCardT3)
            loadUserCardMap(obj, "shardCraftCardT4", session.shardCraftCardT4)

            loadUserAmount(obj, "catFoodTrade", session.catFoodTrade)

            if (obj.has("catFoodTradeSum")) {
                session.catFoodTradeSum = obj.get("catFoodTradeSum").asLong
            }

            if (obj.has("tradeDone")) {
                session.tradeDone = obj.get("tradeDone").asLong
            }

            loadUserAmount(obj, "catFoodSlotMachineInput", session.catFoodSlotMachineInput)
            loadUserAmount(obj, "catFoodSlotMachineReward", session.catFoodSlotMachineReward)

            loadUserAmount(obj, "platinumShardSlotMachineInput", session.platinumShardSlotMachineInput)
            loadUserAmount(obj, "platinumShardSlotMachineReward", session.platinumShardSlotMachineReward)

            loadUserCardMap(obj, "tradedCards", session.tradedCards)

            return session
        }

        private fun loadUserAmount(obj: JsonObject, tag: String, map: HashMap<Long, Long>) {
            if (!obj.has(tag))
                return

            obj.getAsJsonArray(tag).forEach { e ->
                val o = e.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val id = o.get("key").asLong
                    val amount = o.get("val").asLong

                    map[id] = amount
                }
            }
        }

        private fun loadUserCardMap(obj: JsonObject, tag: String, map: HashMap<Long, HashMap<Card, Long>>) {
            if (!obj.has(tag))
                return

            obj.getAsJsonArray(tag).forEach { e ->
                val o = e.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val id = o.get("key").asLong

                    val cards = o.getAsJsonArray("val")

                    val cardMap = HashMap<Card, Long>()

                    cards.forEach { ce ->
                        val co = ce.asJsonObject

                        if (co.has("key") && co.has("val")) {
                            val card = CardData.cards.find { c -> c.unitID == co.get("key").asInt }

                            if (card != null) {
                                cardMap[card] = co.get("val").asLong
                            }
                        }
                    }

                    map[id] = cardMap
                }
            }
        }
    }

    val createdTime: Long

    val activeMembers = HashSet<Long>()

    val catFoodPack = HashMap<Long, Long>()
    val platinumShardPack = HashMap<Long, Long>()

    val generatedCards = HashMap<Long, HashMap<Card, Long>>()

    val removedCardsSalvage = HashMap<Long, HashMap<Card, Long>>()
    val removedCardsManager = HashMap<Long, HashMap<Card, Long>>()

    val removedCards = HashMap<Long, HashMap<Card, Long>>()

    val shardTotal = HashMap<Long, Long>()

    val shardSalvageT1 = HashMap<Long, Long>()
    val shardSalvageT2Regular = HashMap<Long, Long>()
    val shardSalvageT2Seasonal = HashMap<Long, Long>()
    val shardSalvageT2Collaboration = HashMap<Long, Long>()
    val shardSalvageT3 = HashMap<Long, Long>()

    val shardSalvageCardT1 = HashMap<Long, HashMap<Card, Long>>()
    val shardSalvageCardT2Regular = HashMap<Long, HashMap<Card, Long>>()
    val shardSalvageCardT2Seasonal = HashMap<Long, HashMap<Card, Long>>()
    val shardSalvageCardT2Collaboration = HashMap<Long, HashMap<Card, Long>>()
    val shardSalvageCardT3 = HashMap<Long, HashMap<Card, Long>>()

    val shardCraftT2Regular = HashMap<Long, Long>()
    val shardCraftT2Seasonal = HashMap<Long, Long>()
    val shardCraftT2Collaboration = HashMap<Long, Long>()
    val shardCraftT3 = HashMap<Long, Long>()
    val shardCraftT4 = HashMap<Long, Long>()

    val shardCraftCardT2Regular = HashMap<Long, HashMap<Card, Long>>()
    val shardCraftCardT2Seasonal = HashMap<Long, HashMap<Card, Long>>()
    val shardCraftCardT2Collaboration = HashMap<Long, HashMap<Card, Long>>()
    val shardCraftCardT3 = HashMap<Long, HashMap<Card, Long>>()
    val shardCraftCardT4 = HashMap<Long, HashMap<Card, Long>>()

    val catFoodTrade = HashMap<Long, Long>()
    var catFoodTradeSum = 0L
    var tradeDone = 0L

    val tradedCards = HashMap<Long, HashMap<Card, Long>>()

    val catFoodSlotMachineInput = HashMap<Long, Long>()
    val catFoodSlotMachineReward = HashMap<Long, Long>()

    val platinumShardSlotMachineInput = HashMap<Long, Long>()
    val platinumShardSlotMachineReward = HashMap<Long, Long>()

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

    fun logCraft(member: Long, usedShards: Long, cards: List<Card>) {
        val shardMap = when {
            cards[0].isSeasonalUncommon() -> shardCraftT2Seasonal
            cards[0].isCollaborationUncommon() -> shardCraftT2Collaboration
            cards[0].isRegularUncommon() -> shardCraftT2Regular
            else -> {
                when(cards[0].tier) {
                    CardData.Tier.ULTRA -> shardCraftT3
                    else -> shardCraftT4
                }
            }
        }

        shardMap[member] = (shardMap[member] ?: 0) + usedShards

        cards.forEach { c ->
            val cardMap = when {
                c.isSeasonalUncommon() -> shardCraftCardT2Seasonal
                c.isCollaborationUncommon() -> shardCraftCardT2Collaboration
                c.isRegularUncommon() -> shardCraftCardT2Regular
                else -> {
                    when(c.tier) {
                        CardData.Tier.ULTRA -> shardCraftCardT3
                        else -> shardCraftCardT4
                    }
                }
            }

            val map = cardMap.computeIfAbsent(member) { HashMap() }

            map[c] = (map[c] ?: 0) + 1
        }

        shardTotal[member] = (shardTotal[member] ?: 0) - usedShards

        val generatedCardMap = generatedCards.computeIfAbsent(member) { HashMap() }

        cards.forEach { card ->
            generatedCardMap[card] = (generatedCardMap[card] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logManualRoll(member: Long, cards: List<Card>) {
        val cardMap = generatedCards.computeIfAbsent(member) { HashMap() }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logModifyAdd(member: Long, cards: List<Card>) {
        val cardMap = generatedCards.computeIfAbsent(member) { HashMap() }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logModifyRemove(member: Long, cards: List<Card>) {
        val cardMap = removedCards.computeIfAbsent(member) { HashMap() }
        val managerMap = removedCardsManager.computeIfAbsent(member) { HashMap() }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
            managerMap[it] = (managerMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logRoll(member: Long, pack: CardPack, cards: List<Card>) {
        if (pack.cost.catFoods != 0L)
            catFoodPack[member] = (catFoodPack[member] ?: 0) + pack.cost.catFoods

        if (pack.cost.platinumShards != 0L)
            platinumShardPack[member] = (platinumShardPack[member] ?: 0) + pack.cost.platinumShards

        val cardMap = generatedCards.computeIfAbsent(member) { HashMap() }

        cards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
        }

        activeMembers.add(member)
    }

    fun logSalvage(member: Long, usedCards: List<Card>, shard: Long) {
        val shardMap = when {
            usedCards[0].isSeasonalUncommon() -> shardSalvageT2Seasonal
            usedCards[0].isCollaborationUncommon() -> shardSalvageT2Collaboration
            usedCards[0].isRegularUncommon() -> shardSalvageT2Regular
            else -> {
                when(usedCards[0].tier) {
                    CardData.Tier.COMMON -> shardSalvageT1
                    else -> shardSalvageT3
                }
            }
        }

        shardMap[member] = (shardMap[member] ?: 0) + shard

        usedCards.forEach { c ->
            val cardMap = when {
                c.isSeasonalUncommon() -> shardSalvageCardT2Seasonal
                c.isCollaborationUncommon() -> shardSalvageCardT2Collaboration
                c.isRegularUncommon() -> shardSalvageCardT2Regular
                else -> {
                    when(c.tier) {
                        CardData.Tier.COMMON -> shardSalvageCardT1
                        else -> shardSalvageCardT3
                    }
                }
            }

            val map = cardMap.computeIfAbsent(member) { HashMap() }

            map[c] = (map[c] ?: 0) + 1
        }

        shardTotal[member] = (shardTotal[member] ?: 0) + shard

        val cardMap = removedCards.computeIfAbsent(member) { HashMap() }
        val removedMap = removedCardsSalvage.computeIfAbsent(member) { HashMap() }

        usedCards.forEach {
            cardMap[it] = (cardMap[it] ?: 0) + 1
            removedMap[it] = (removedMap[it] ?: 0) + 1
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

        session.suggestion[0].cards.forEach { (card, amount) ->
            firstTraderMap[card] = (firstTraderMap[card] ?: 0) - amount
            secondTraderMap[card] = (secondTraderMap[card] ?: 0) + amount
        }

        session.suggestion[1].cards.forEach { (card, amount) ->
            firstTraderMap[card] = (firstTraderMap[card] ?: 0) + amount
            secondTraderMap[card] = (secondTraderMap[card] ?: 0) - amount
        }

        tradeDone++
    }

    fun logMassShardModify(members: List<Long>, amount: Long) {
        members.forEach {
            shardTotal[it] = (shardTotal[it] ?: 0) + amount
        }
    }

    fun logSlotMachineFail(user: Long, input: Long, compensation: Long) {
        activeMembers.add(user)

        catFoodSlotMachineInput[user] = (catFoodSlotMachineInput[user] ?: 0) + input
        catFoodSlotMachineReward[user] = (catFoodSlotMachineReward[user] ?: 0) + compensation
    }

    fun logSlotMachineWin(user: Long, input: Long, reward: Long, cards: List<Card>, entryType: SlotEntryFee.EntryType) {
        activeMembers.add(user)

        when(entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> {
                catFoodSlotMachineInput[user] = (catFoodSlotMachineInput[user] ?: 0) + input
                catFoodSlotMachineReward[user] = (catFoodSlotMachineReward[user] ?: 0) + reward
            }
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> {
                platinumShardSlotMachineInput[user] = (platinumShardSlotMachineInput[user] ?: 0) + input
                platinumShardSlotMachineReward[user] = (platinumShardSlotMachineReward[user] ?: 0) + reward
            }
        }

        val cardMap = generatedCards.computeIfAbsent(user) { HashMap() }

        cards.forEach { c ->
            cardMap[c] = (cardMap[c] ?: 0) + 1
        }
    }

    fun saveSessionAsFile() {
        val folder = File(if (CardBot.test) "./data/testCardLog" else "./data/cardLog")

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

        saveUserAmount(obj, "catFoodPack", catFoodPack)

        saveUserCardMap(obj, "generatedCards", generatedCards)

        saveUserCardMap(obj, "removedCardsSalvage", removedCardsSalvage)
        saveUserCardMap(obj, "removedCardsManager", removedCardsManager)

        saveUserCardMap(obj, "removedCards", removedCards)

        saveUserAmount(obj, "shardTotal", shardTotal)

        saveUserAmount(obj, "shardSalvageT1", shardSalvageT1)
        saveUserAmount(obj, "shardSalvageT2Regular", shardSalvageT2Regular)
        saveUserAmount(obj, "shardSalvageT2Seasonal", shardSalvageT2Seasonal)
        saveUserAmount(obj, "shardSalvageT2Collaboration", shardSalvageT2Collaboration)
        saveUserAmount(obj, "shardSalvageT3", shardSalvageT3)

        saveUserCardMap(obj, "shardSalvageCardT1", shardSalvageCardT1)
        saveUserCardMap(obj, "shardSalvageCardT2Regular", shardSalvageCardT2Regular)
        saveUserCardMap(obj, "shardSalvageCardT2Seasonal", shardSalvageCardT2Seasonal)
        saveUserCardMap(obj, "shardSalvageCardT2Collaboration", shardSalvageCardT2Collaboration)
        saveUserCardMap(obj, "shardSalvageCardT3", shardSalvageCardT3)

        saveUserAmount(obj, "shardCraftT2Regular", shardCraftT2Regular)
        saveUserAmount(obj, "shardCraftT2Seasonal", shardCraftT2Seasonal)
        saveUserAmount(obj, "shardCraftT2Collaboration", shardCraftT2Collaboration)
        saveUserAmount(obj, "shardCraftT3", shardCraftT3)
        saveUserAmount(obj, "shardCraftT4", shardCraftT4)

        saveUserCardMap(obj, "shardCraftCardT2Regular", shardCraftCardT2Regular)
        saveUserCardMap(obj, "shardCraftCardT2Seasonal", shardCraftCardT2Seasonal)
        saveUserCardMap(obj, "shardCraftCardT2Collaboration", shardCraftCardT2Collaboration)
        saveUserCardMap(obj, "shardCraftCardT3", shardCraftCardT3)
        saveUserCardMap(obj, "shardCraftCardT4", shardCraftCardT4)

        saveUserAmount(obj, "catFoodTrade", catFoodTrade)
        obj.addProperty("catFoodTradeSum", catFoodTradeSum)
        obj.addProperty("tradeDone", tradeDone)

        saveUserAmount(obj, "catFoodSlotMachineInput", catFoodSlotMachineInput)
        saveUserAmount(obj, "catFoodSlotMachineReward", catFoodSlotMachineReward)

        saveUserAmount(obj, "platinumShardSlotMachineInput", platinumShardSlotMachineInput)
        saveUserAmount(obj, "platinumShardSlotMachineReward", platinumShardSlotMachineReward)

        return obj
    }

    private fun saveUserAmount(obj: JsonObject, tag: String, map: Map<Long, Long>) {
        val arr = JsonArray()

        map.forEach { (id, amount) ->
            val o = JsonObject()

            o.addProperty("key", id)
            o.addProperty("val", amount)

            arr.add(o)
        }

        obj.add(tag, arr)
    }

    private fun saveUserCardMap(obj: JsonObject, tag: String, map: Map<Long, Map<Card, Long>>) {
        val arr = JsonArray()

        map.forEach { (id, cardMap) ->
            val o = JsonObject()

            o.addProperty("key", id)

            val cardArr = JsonArray()

            cardMap.forEach { (card, amount) ->
                val co = JsonObject()

                co.addProperty("key", card.unitID)
                co.addProperty("val", amount)

                cardArr.add(co)
            }

            o.add("val", cardArr)

            arr.add(o)
        }

        obj.add(tag, arr)
    }
}