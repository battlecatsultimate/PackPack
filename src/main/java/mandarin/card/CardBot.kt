package mandarin.card

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import common.CommonStatic
import mandarin.card.commands.*
import mandarin.card.supporter.*
import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.card.supporter.transaction.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.Initializer
import mandarin.packpack.supporter.PackContext
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

object CardBot : ListenerAdapter() {
    var globalPrefix = "cd."
    private var test = false

    private var ready = false

    @JvmStatic
    fun main(args: Array<String>) {
        initialize()

        args.forEachIndexed { index, arg ->
            if (arg == "--test" && index < args.size - 1 && args[index + 1] == "true") {
                test = true
                globalPrefix = "ct."
            }
        }

        TatsuHandler.API = args[1]

        val token = args[0]
        val builder = JDABuilder.createDefault(token)

        builder.setEnabledIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.SCHEDULED_EVENTS
        )

        builder.disableCache(CacheFlag.VOICE_STATE)
        builder.setActivity(Activity.playing(if (test) "B" else "A"))
        builder.addEventListeners(CardBot)

        val client = builder.build()

        StaticStore.logger.assignClient(client)
        StaticStore.saver = Timer()

        StaticStore.saver.schedule(object : TimerTask() {
            override fun run() {
                saveCardData()
            }
        }, 0, TimeUnit.MINUTES.toMillis(1))
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!ready)
            return

        val m = event.member ?: return
        val ch = event.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isDealer(m) && !CardData.isAllowed(ch))
            return

        val segments = event.message.contentRaw.lowercase().split(" ")

        val firstSegment = if (segments.isEmpty())
            ""
        else
            segments[0]

        when(firstSegment) {
            "${globalPrefix}save" -> Save().execute(event)
            "${globalPrefix}rollmanual",
            "${globalPrefix}rm" -> RollManual().execute(event)
            "${globalPrefix}trade" -> Trade().execute(event)
            "${globalPrefix}trademanual",
            "${globalPrefix}tm" -> TradeManual().execute(event)
            "${globalPrefix}cards" -> Cards().execute(event)
            "${globalPrefix}purchase",
            "${globalPrefix}buy" -> Buy().execute(event)
            "${globalPrefix}roll" -> Roll().execute(event)
            "${globalPrefix}moidfyinventory",
            "${globalPrefix}modify",
            "${globalPrefix}mi"-> ModifyInventory().execute(event)
            "${globalPrefix}activate" -> Activate().execute(event)
            "${globalPrefix}equip" -> Equip().execute(event)
            "${globalPrefix}checkt3",
            "${globalPrefix}t3" -> Check(CardData.Tier.ULTRA).execute(event)
            "${globalPrefix}checkt4",
            "${globalPrefix}t4" -> Check(CardData.Tier.LEGEND).execute(event)
        }

        val session = findSession(event.channel.idLong) ?: return

        when(firstSegment) {
            "${globalPrefix}suggest" -> Suggest(session).execute(event)
            "${globalPrefix}confirm" -> Confirm(session).execute(event)
            "${globalPrefix}cancel" -> Cancel(session).execute(event)
        }
    }

    override fun onGenericInteractionCreate(event: GenericInteractionCreateEvent) {
        super.onGenericInteractionCreate(event)

        val u = event.user

        when(event) {
            is ModalInteractionEvent,
            is GenericComponentInteractionCreateEvent-> {
                StaticStore.getHolderHub(u.id)?.handleEvent(event)
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)

        EmojiStore.initialize(event.jda)

        TransactionLogger.logChannel = event.jda.getGuildChannelById(CardData.transactionLog) as MessageChannel
        TransactionLogger.tradeChannel = event.jda.getGuildChannelById(CardData.tradingLog) as MessageChannel
        TransactionLogger.modChannel = event.jda.getGuildChannelById(CardData.modLog) as MessageChannel

        StaticStore.loggingChannel = ServerData.get("loggingChannel")
        StaticStore.logger.assignClient(event.jda)

        ready = true
    }

    private fun initialize() {
        CommonStatic.ctx = PackContext()
        CommonStatic.getConfig().ref = false
        CommonStatic.getConfig().updateOldMusic = false

        readCardData()

        Initializer.checkAssetDownload()
    }

    fun readCardData() {
        val cardFolder = File("./data/cards")

        if (!cardFolder.exists())
            return

        val tiers = cardFolder.listFiles() ?: return

        for(t in tiers) {
            val cards = t.listFiles() ?: continue

            cards.sortBy { it.name.split("-")[0].toInt() }

            for(card in cards) {
                val tier = when(t.name) {
                    "Tier 1" -> CardData.Tier.COMMON
                    "Tier 2" -> CardData.Tier.UNCOMMON
                    "Tier 3" -> CardData.Tier.ULTRA
                    "Tier 4" -> CardData.Tier.LEGEND
                    else -> throw IllegalStateException("Invalid tier type ${t.name} found")
                }

                val nameData = card.name.replace(".png", "").split(Regex("-"), 2)

                CardData.cards.add(Card(nameData[0].toInt(), tier, nameData[1], card))
            }
        }

        CardData.cards.removeIf { c ->
            val result = !CardData.bannerData.any { t -> t.any { b -> c.tier == CardData.Tier.LEGEND || c.unitID in b } }

            if (result) {
                println("Removing ${c.unitID}")
            }

            result
        }

        CardData.common.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.COMMON }.filter { r -> CardData.permanents[0].any { i -> r.unitID in CardData.bannerData[0][i] } })
        CardData.uncommon.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.UNCOMMON }.filter { r -> CardData.permanents[1].any { i -> r.unitID in CardData.bannerData[1][i] } })
        CardData.ultraRare.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.ULTRA }.filter { r -> CardData.permanents[2].any { i -> r.unitID in CardData.bannerData[2][i] } })
        CardData.legendRare.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.LEGEND }.filter { r -> CardData.bannerData[3].any { arr -> r.unitID !in arr } })

        val serverElement: JsonElement? = StaticStore.getJsonFile("serverinfo")

        if (serverElement != null && serverElement.isJsonObject) {
            val serverInfo = serverElement.asJsonObject

            if (serverInfo.has("lang")) {
                StaticStore.langs = StaticStore.jsonToMapString(serverInfo.getAsJsonArray("lang"))
            }
        }

        val element: JsonElement? = StaticStore.getJsonFile("cardSave")

        if (element == null || !element.isJsonObject)
            return

        val obj = element.asJsonObject

        if (obj.has("inventory")) {
            val arr = obj.getAsJsonArray("inventory")

            for (i in 0 until arr.size()) {
                val pair = arr[i].asJsonObject

                if (!pair.has("key") || !pair.has("val"))
                    continue

                val key = pair["key"].asString

                val value = Inventory.readInventory(pair["val"].asJsonObject)

                CardData.inventories[key] = value
            }

            CardData.inventories.values.forEach { i -> i.cards.entries.removeIf { e -> e.value <= 0 } }
        }

        if (obj.has("sessions")) {
            obj.getAsJsonArray("sessions").forEach { CardData.sessions.add(TradingSession.fromJson(it.asJsonObject)) }
        }

        if (obj.has("sessionNumber")) {
            CardData.sessionNumber = obj.get("sessionNumber").asLong
        }

        if (obj.has("leftRequest")) {
            TatsuHandler.leftRequest = obj.get("leftRequest").asInt
        }

        if (obj.has("nextRefreshTime")) {
            TatsuHandler.nextRefreshTime = obj.get("nextRefreshTime").asLong
        }

        if (obj.has("activatedBanners")) {
            CardData.activatedBanners.addAll(
                obj.getAsJsonArray("activatedBanners").map {
                    Activator.valueOf(it.asString)
                }
            )
        }

        if (obj.has("cooldown")) {
            obj.getAsJsonArray("cooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val value = o.getAsJsonArray("val")

                    CardData.cooldown[o.get("key").asString] = longArrayOf(value[0].asLong, value[1].asLong)
                }
            }
        }

        if (obj.has("tradeCooldown")) {
            obj.getAsJsonArray("tradeCooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    var value = o.get("val").asLong

                    if (value - CardData.getUnixEpochTime() > CardData.tradeCatFoodCooldownTerm) {
                        value = value - 1 * 24 * 60 * 60 * 1000 + CardData.tradeCatFoodCooldownTerm
                    }

                    CardData.tradeCooldown[o.get("key").asString] = value
                }
            }
        }

        if (obj.has("tradeTrialCooldown")) {
            obj.getAsJsonArray("tradeTrialCooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    CardData.tradeTrialCooldown[o.get("key").asString] = o.get("val").asLong
                }
            }
        }
    }

    @Synchronized
    fun saveCardData() {
        val obj = JsonObject()
        val inventory = JsonArray()

        for (key in CardData.inventories.keys) {
            val inv = CardData.inventories[key] ?: continue

            val pair = JsonObject()

            pair.addProperty("key", key)
            pair.add("val", inv.toJson())

            inventory.add(pair)
        }

        obj.add("inventory", inventory)

        val sessions = JsonArray()

        CardData.sessions.forEach { sessions.add(it.toJson()) }

        obj.add("sessions", sessions)

        obj.addProperty("sessionNumber", CardData.sessionNumber)

        obj.addProperty("leftRequest", TatsuHandler.leftRequest)
        obj.addProperty("nextRefreshTime", TatsuHandler.nextRefreshTime)

        val activators = JsonArray()

        CardData.activatedBanners.forEach {
            activators.add(it.name)
        }

        obj.add("activatedBanners", activators)

        val cooldown = JsonArray()

        CardData.cooldown.keys.forEach {
            val cd = CardData.cooldown[it]

            if (cd != null) {
                val o = JsonObject()

                o.addProperty("key", it)

                val value = JsonArray()

                value.add(cd[0])
                value.add(cd[1])

                o.add("val", value)

                cooldown.add(o)
            }
        }

        obj.add("cooldown", cooldown)

        val tradeCoolDown = JsonArray()

        CardData.tradeCooldown.keys.forEach {
            val cd = CardData.tradeCooldown[it]

            if (cd != null) {
                val o = JsonObject()

                o.addProperty("key", it)
                o.addProperty("val", cd)

                tradeCoolDown.add(o)
            }
        }

        obj.add("tradeCooldown", tradeCoolDown)

        val tradeTrialCooldown = JsonArray()

        CardData.tradeTrialCooldown.keys.forEach {
            val cd = CardData.tradeTrialCooldown[it]

            if (cd != null) {
                val o = JsonObject()

                o.addProperty("key", it)
                o.addProperty("val", cd)

                tradeTrialCooldown.add(o)
            }
        }

        obj.add("tradeCooldown", tradeTrialCooldown)

        try {
            val folder = File("./data/")

            if (!folder.exists()) {
                val res = folder.mkdirs()

                if (!res) {
                    println("Can't create folder " + folder.absolutePath)
                    return
                }
            }

            val file = File(folder.absolutePath, "cardSave.json")

            if (!file.exists()) {
                val res = file.createNewFile()
                if (!res) {
                    println("Can't create file " + file.absolutePath)
                    return
                }
            }

            val mapper = ObjectMapper()

            mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

            val json = obj.toString()

            val tree = mapper.readTree(json)

            val writer = FileWriter("./data/cardSave.json")

            writer.append(mapper.writeValueAsString(tree))
            writer.close()
        } catch (e: IOException) {
            StaticStore.logger.uploadErrorLog(e, "Failed to save card save")
        }
    }

    private fun findSession(channelID: Long) : TradingSession? {
        return CardData.sessions.find { s -> s.postID == channelID }
    }
}
