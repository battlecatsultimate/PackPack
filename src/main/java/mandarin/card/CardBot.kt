package mandarin.card

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import common.CommonStatic
import mandarin.card.commands.*
import mandarin.card.supporter.*
import mandarin.card.supporter.log.LogSession
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.*
import mandarin.packpack.supporter.server.data.ShardLoader
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.exceptions.ContextException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

object CardBot : ListenerAdapter() {
    var globalPrefix = "cd."
    var test = false

    private var ready = false
    private var notifier = 0
    private var collectorMonitor = 0
    private var backup = 360

    var locked = false
    var rollLocked = false
    var forceReplace = false

    @JvmStatic
    fun main(args: Array<String>) {
        initialize()

        args.forEachIndexed { index, arg ->
            if (arg == "--test" && index < args.size - 1 && args[index + 1] == "true") {
                test = true
                globalPrefix = "ct."
            }
        }

        val token = args[0]
        val builder = DefaultShardManagerBuilder.createDefault(token)

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
                if (notifier == 2 && !test) {
                    notifier = 0

                    val removeQueue = ArrayList<String>()

                    CardData.notifierGroup.forEach { n ->
                        client.retrieveUserById(n).queue { u ->
                            val packList = StringBuilder()

                            val cooldown = CardData.cooldown[u.id] ?: return@queue

                            val currentTime = CardData.getUnixEpochTime()

                            cooldown.forEachIndexed { i, c ->
                                if (c > 0 && c - currentTime <= 0) {
                                    val packName = when (i) {
                                        CardData.LARGE -> "Large Pack"
                                        CardData.SMALL -> "Small Pack"
                                        CardData.PREMIUM -> "Premium Pack"
                                        else -> ""
                                    }

                                    packList.append("- ")
                                            .append(packName)
                                            .append("\n")
                                }
                            }

                            if (packList.isNotBlank()) {
                                u.openPrivateChannel().queue({ private ->
                                    private.sendMessage("You can roll pack below!\n\n$packList").queue({
                                        for (i in cooldown.indices) {
                                            if (cooldown[i] > 0 && cooldown[i] - currentTime <= 0)
                                                cooldown[i] = 0
                                        }
                                    }, { e ->
                                        if (e is ContextException) {
                                            removeQueue.add(n)
                                        }
                                    })
                                }, { _ ->
                                    removeQueue.add(n)
                                })
                            }
                        }
                    }

                    if (removeQueue.isNotEmpty()) {
                        CardData.notifierGroup.removeAll(removeQueue.toSet())
                    }

                    RecordableThread.handleExpiration()
                } else {
                    notifier++
                }

                if (collectorMonitor == 30 && !test) {
                    collectorMonitor = 0

                    CardData.inventories.keys.forEach { userID ->
                        val inventory = Inventory.getInventory(userID)

                        if (!inventory.validForLegendCollector() && inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                            inventory.vanityRoles.remove(CardData.Role.LEGEND)

                            client.retrieveUserById(userID).queue { u ->
                                u.openPrivateChannel().queue({ privateChannel ->
                                    privateChannel.sendMessage("Your Legendary Collector role has been removed from your inventory. There are 2 possible reasons for this decision\n\n" +
                                            "1. You spent your card on trading, crafting, etc. so you don't meet condition of legendary collector now\n" +
                                            "2. New cards have been added, so you have to collect those cards to retrieve role back\n\n" +
                                            "This is automated system. Please contact card managers if this seems to be incorrect automation").queue(null) { _ -> }
                                }, { _ -> })
                            }
                        }
                    }
                } else {
                    collectorMonitor++
                }

                if (!test && backup == 360) {
                    backup = 0

                    client.retrieveUserById(StaticStore.MANDARIN_SMELL)
                        .queue {user ->
                            user.openPrivateChannel().queue { pv ->
                                pv.sendMessage("Sending backup")
                                    .addFiles(FileUpload.fromData(File("./data/cardSave.json")))
                                    .queue()
                            }
                        }

                    client.retrieveUserById(ServerData.get("gid")).queue { user ->
                        user.openPrivateChannel().queue {pv ->
                            pv.sendMessage("Sending backup")
                                .addFiles(FileUpload.fromData(File("./data/cardSave.json")))
                                .queue()
                        }
                    }
                } else {
                    backup++
                }

                if (!forceReplace) {
                    saveCardData()
                }

                LogSession.syncSession()
            }
        }, 0, TimeUnit.MINUTES.toMillis(1))
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!ready)
            return

        val m = event.member ?: return
        val ch = event.channel

        val lastTime = CardData.lastMessageSent[m.id] ?: 0L
        val currentTime = CardData.getUnixEpochTime()

        if (currentTime - lastTime >= CardData.catFoodCooldown && ch.id !in CardData.excludedCatFoodChannel) {
            CardData.lastMessageSent[m.id] = currentTime

            val inventory = Inventory.getInventory(m.id)

            inventory.catFoods += if (CardData.minimumCatFoods != CardData.maximumCatFoods) {
                CardData.minimumCatFoods.rangeTo(CardData.maximumCatFoods).random()
            } else {
                CardData.maximumCatFoods
            }
        }

        if (CardData.isBanned(m))
            return

        val segments = event.message.contentRaw.lowercase().split(" ")

        val firstSegment = if (segments.isEmpty())
            ""
        else
            segments[0]

        when(firstSegment) {
            "${globalPrefix}catfood",
            "${globalPrefix}cf" -> {
                CatFood().execute(event)

                return
            }
            "${globalPrefix}transfercatfood",
            "${globalPrefix}tcf" -> {
                TransferCatFood().execute(event)

                return
            }
            "${globalPrefix}rank" -> {
                Rank().execute(event)

                return
            }
        }

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m) && !CardData.isAllowed(ch))
            return

        if (locked && m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        when(firstSegment) {
            "${globalPrefix}save" -> Save().execute(event)
            "${globalPrefix}rollmanual",
            "${globalPrefix}rm" -> RollManual().execute(event)
            "${globalPrefix}trade",
            "${globalPrefix}tr" -> Trade().execute(event)
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
            "${globalPrefix}approve" -> Approve().execute(event)
            "${globalPrefix}notice" -> Notice().execute(event)
            "${globalPrefix}poolt1",
            "${globalPrefix}p1" -> Pool(CardData.Tier.COMMON).execute(event)
            "${globalPrefix}poolt2",
            "${globalPrefix}p2" -> Pool(CardData.Tier.UNCOMMON).execute(event)
            "${globalPrefix}poolt3",
            "${globalPrefix}p3" -> Pool(CardData.Tier.ULTRA).execute(event)
            "${globalPrefix}poolt4",
            "${globalPrefix}p4" -> Pool(CardData.Tier.LEGEND).execute(event)
            "${globalPrefix}salvage" -> {
                if (test) {
                    Salvage().execute(event)
                }
            }
            "${globalPrefix}lock" -> Lock().execute(event)
            "${globalPrefix}unlock" -> Unlock().execute(event)
            "${globalPrefix}craft" -> {
                if (test) {
                    Craft().execute(event)
                }
            }
            "${globalPrefix}report" -> Report().execute(event)
            "${globalPrefix}savedata" -> SaveData().execute(event)
            "${globalPrefix}replacesave" -> ReplaceSave().execute(event)
            "${globalPrefix}catfoodrate",
            "${globalPrefix}cfr" -> CatFoodRate().execute(event)
            "${globalPrefix}excludechannel",
            "${globalPrefix}ec" -> ExcludeChannel().execute(event)
            "${globalPrefix}catfood",
            "${globalPrefix}cf" -> CatFood().execute(event)
            "${globalPrefix}transfercatfood",
            "${globalPrefix}tcf" -> TransferCatFood().execute(event)
            "${globalPrefix}masscatfood",
            "${globalPrefix}mcf" -> MassCatFood().execute(event)
            "${globalPrefix}hack" -> {
                if (test) {
                    Hack().execute(event)
                }
            }
        }

        val session = findSession(event.channel.idLong) ?: return

        when(firstSegment) {
            "${globalPrefix}suggest",
            "${globalPrefix}su" -> Suggest(session).execute(event)
            "${globalPrefix}confirm" -> Confirm(session).execute(event)
            "${globalPrefix}cancel" -> Cancel(session).execute(event)
        }
    }

    override fun onGenericInteractionCreate(event: GenericInteractionCreateEvent) {
        super.onGenericInteractionCreate(event)

        val m = event.member ?: return

        if (CardData.isBanned(m))
            return

        if (locked && !CardData.hasAllPermission(m))
            return

        when(event) {
            is ModalInteractionEvent,
            is GenericComponentInteractionCreateEvent-> {
                StaticStore.getHolderHub(m.id)?.handleEvent(event)
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)

        EmojiStore.initialize(ShardLoader(event.jda.shardManager))

        TransactionLogger.logChannel = event.jda.getGuildChannelById(CardData.transactionLog) as MessageChannel
        TransactionLogger.tradeChannel = event.jda.getGuildChannelById(CardData.tradingLog) as MessageChannel
        TransactionLogger.modChannel = event.jda.getGuildChannelById(CardData.modLog) as MessageChannel
        TransactionLogger.catFoodChannel = event.jda.getGuildChannelById(CardData.catFoodLog) as MessageChannel

        StaticStore.loggingChannel = ServerData.get("loggingChannel")

        val manager = event.jda.shardManager

        if (manager != null) {
            StaticStore.logger.assignClient(manager)
        }

        ready = true
    }

    private fun initialize() {
        CommonStatic.ctx = PackContext()
        CommonStatic.getConfig().ref = false
        CommonStatic.getConfig().updateOldMusic = false

        readCardData()

        Initializer.checkAssetDownload(false)
    }

    private fun readCardData() {
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

        if (obj.has("locked")) {
            locked = obj.get("locked").asBoolean
        }

        if (obj.has("rollLocked")) {
            rollLocked = obj.get("rollLocked").asBoolean
        }

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

        if (obj.has("activatedBanners")) {
            CardData.activatedBanners.addAll(
                obj.getAsJsonArray("activatedBanners").map {
                    Activator.valueOf(it.asString)
                }
            )
        }

        if (obj.has("notifierGroup")) {
            val arr = obj.getAsJsonArray("notifierGroup")

            arr.forEach { e ->
                CardData.notifierGroup.add(e.asString)
            }
        }

        if (obj.has("cooldown")) {
            obj.getAsJsonArray("cooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val value = o.getAsJsonArray("val")

                    val arr = longArrayOf(-1, -1, -1)

                    value.forEachIndexed { i, v ->
                        arr[i] = v.asLong
                    }

                    CardData.cooldown[o.get("key").asString] = arr
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

        if (obj.has("minimumCatFoods")) {
            CardData.minimumCatFoods = obj.get("minimumCatFoods").asLong
        }

        if (obj.has("maximumCatFoods")) {
            CardData.maximumCatFoods = obj.get("maximumCatFoods").asLong
        }

        if (obj.has("catFoodCooldown")) {
            CardData.catFoodCooldown = obj.get("catFoodCooldown").asLong
        }

        if (obj.has("lastMessageSent")) {
            val arr = obj.getAsJsonArray("lastMessageSent")

            arr.forEach { e ->
                val o = e.asJsonObject

                if (o.has("key") && o.has("val")) {
                    CardData.lastMessageSent[o.get("key").asString] = o.get("val").asLong
                }
            }
        }

        if (obj.has("excludedCatFoodChannel")) {
            val arr = obj.getAsJsonArray("excludedCatFoodChannel")

            arr.forEach { e ->
                CardData.excludedCatFoodChannel.add(e.asString)
            }
        }
    }

    @Synchronized
    fun saveCardData() {
        val obj = JsonObject()

        obj.addProperty("locked", locked)
        obj.addProperty("rollLocked", rollLocked)

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
                value.add(cd[2])

                o.add("val", value)

                cooldown.add(o)
            }
        }

        obj.add("cooldown", cooldown)

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

        val notifierGroup = JsonArray()

        CardData.notifierGroup.forEach { id ->
            notifierGroup.add(id)
        }

        obj.add("notifierGroup", notifierGroup)

        obj.addProperty("minimumCatFoods", CardData.minimumCatFoods)
        obj.addProperty("maximumCatFoods", CardData.maximumCatFoods)

        obj.addProperty("catFoodCooldown", CardData.catFoodCooldown)

        val lastMessageSent = JsonArray()

        CardData.lastMessageSent.forEach { (id, timeStamp) ->
            val o = JsonObject()

            o.addProperty("key", id)
            o.addProperty("val", timeStamp)

            lastMessageSent.add(o)
        }

        obj.add("lastMessageSent", lastMessageSent)

        val excludedCatFoodChannel = JsonArray()

        CardData.excludedCatFoodChannel.forEach { id ->
            excludedCatFoodChannel.add(id)
        }

        obj.add("excludedCatFoodChannel", excludedCatFoodChannel)

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
