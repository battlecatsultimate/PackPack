package mandarin.card

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import common.CommonStatic
import mandarin.card.commands.*
import mandarin.card.supporter.*
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.log.LogSession
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.slot.SlotEmojiContainer
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.*
import mandarin.packpack.supporter.bc.DataToString
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.data.ShardLoader
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.cache.CacheFlag
import net.dv8tion.jda.internal.requests.RestActionImpl
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object CardBot : ListenerAdapter() {
    var globalPrefix = "cd."
    var test = false

    private var ready = false
    private var notifier = 2
    private var collectorMonitor = 30
    private var backup = 360

    var locked = false
    var rollLocked = false
    var forceReplace = false
    var inviteLocked = false

    @JvmStatic
    fun main(args: Array<String>) {
        Runtime.getRuntime().addShutdownHook(Thread { Logger.writeLog(Logger.BotInstance.CARD_DEALER) })
        Thread.currentThread().uncaughtExceptionHandler = object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread?, e: Throwable?) {
                if (e == null) {
                    StaticStore.logger.uploadLog("E/CardBot::main - Uncaught exception found without trace : ${t?.name}")
                } else {
                    StaticStore.logger.uploadErrorLog(e, "E/CardBot::main - Uncaught exception found : ${t?.name}")
                }
            }
        }

        RestActionImpl.setDefaultFailure { e ->
            StaticStore.logger.uploadErrorLog(e, "E/Unknown - Failed to perform the task")
        }

        args.forEachIndexed { index, arg ->
            if (arg == "--test" && index < args.size - 1 && args[index + 1] == "true") {
                test = true
                globalPrefix = "ct."
            }
        }

        initialize()

        val token = args[0]
        val builder = DefaultShardManagerBuilder.createDefault(token)

        builder.setEnabledIntents(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS,
            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.SCHEDULED_EVENTS
        )

        builder.disableCache(CacheFlag.VOICE_STATE)
        builder.setActivity(Activity.playing(if (test) "B" else "A"))
        builder.addEventListeners(CardBot)
        builder.setEnableShutdownHook(true)

        val client = builder.build()

        StaticStore.logger.assignClient(client)
        StaticStore.saver = Timer()

        StaticStore.saver.schedule(object : TimerTask() {
            override fun run() {
                try {
                    val currentTime = CardData.getUnixEpochTime()

                    if (notifier == 2) {
                        notifier = 1

                        val removeQueue = ArrayList<Long>()

                        CardData.notifierGroup.filter { (_, notifier) -> notifier.any { b -> b } }.forEach { (id, notifier) ->
                            if (!test || id.toString() == StaticStore.MANDARIN_SMELL) {
                                val messageContent = StringBuilder()

                                if (notifier[0]) {
                                    val packList = StringBuilder()

                                    val cooldown = CardData.cooldown[id] ?: return@forEach

                                    cooldown.forEach { (uuid, cd) ->
                                        val pack = CardData.cardPacks.find { pack -> pack.uuid == uuid }

                                        if (pack != null && cd > 0 && cd - currentTime <= 0 && pack.activated) {
                                            packList.append("- ")
                                                .append(pack.packName)
                                                .append("\n")
                                        }
                                    }

                                    if (packList.isNotBlank()) {
                                        messageContent.append("You can roll pack below!\n\n")
                                            .append(packList)
                                    }
                                }

                                if (notifier[1]) {
                                    val slotList = StringBuilder()

                                    val cooldown = CardData.slotCooldown[id] ?: return@forEach

                                    cooldown.forEach { (uuid, cd) ->
                                        val slot = CardData.slotMachines.filter { slot -> slot.cooldown >= CardData.MINIMUM_NOTIFY_TIME }.find { slot -> slot.uuid == uuid }

                                        if (slot != null && cd > 0 && cd - currentTime <= 0 && slot.activate) {
                                            slotList.append("- ")
                                                .append(slot.name)
                                                .append("\n")
                                        }
                                    }

                                    if (slotList.isNotBlank()) {
                                        if (messageContent.isNotBlank()) {
                                            messageContent.append("\n")
                                        }

                                        messageContent.append("You can roll slot machine below!\n\n")
                                            .append(slotList)
                                    }
                                }

                                //TODO(Resolve private message sending problem)
//                                if (messageContent.isNotBlank()) {
//                                    client.retrieveUserById(id).queue { u ->
//                                        u.openPrivateChannel().queue { pc ->
//                                            pc.sendMessage(messageContent).queue()
//                                        }
//                                    }
//                                }

                                CardData.cooldown.forEach { (_, cooldownMap) ->
                                    cooldownMap.forEach { (uuid, cd) ->
                                        val pack = CardData.cardPacks.find { p -> p.uuid == uuid } ?: return@forEach

                                        if (cd > 0 && cd - currentTime <= 0 && pack.activated) {
                                            cooldownMap[uuid] = 0
                                        }
                                    }
                                }

                                CardData.slotCooldown.forEach { (_, cooldownMap) ->
                                    cooldownMap.forEach { (uuid, cd) ->
                                        val slot = CardData.slotMachines.find { s -> s.uuid == uuid } ?: return@forEach

                                        if (cd > 0 && cd - currentTime <= 0 && slot.activate) {
                                            cooldownMap[uuid] = 0
                                        }
                                    }
                                }
                            }
                        }

                        removeQueue.forEach { id -> CardData.notifierGroup.remove(id) }

                        RecordableThread.handleExpiration()
                    } else {
                        notifier++
                    }

                    if (collectorMonitor == 30 && !test) {
                        collectorMonitor = 1

                        val g = client.getGuildById(CardData.guild)
                        val collectorRole = g?.roles?.find { r -> r.id == CardData.Role.LEGEND.id }

                        CardData.inventories.keys.forEach { userID ->
                            val inventory = Inventory.getInventory(userID)

                            if (!inventory.validForLegendCollector() && inventory.vanityRoles.contains(CardData.Role.LEGEND)) {
                                inventory.vanityRoles.remove(CardData.Role.LEGEND)

                                if (collectorRole != null) {
                                    g.removeRoleFromMember(UserSnowflake.fromId(userID), collectorRole).queue()
                                }

                                //TODO(Resolve private message sending problem)
//                                client.retrieveUserById(userID).queue { u ->
//                                    u.openPrivateChannel().queue({ privateChannel ->
//                                        privateChannel.sendMessage("Your Legendary Collector role has been removed from your inventory. There are 2 possible reasons for this decision\n\n" +
//                                                "1. You spent your card on trading, crafting, etc. so you don't meet condition of legendary collector now\n" +
//                                                "2. New cards have been added, so you have to collect those cards to retrieve role back\n\n" +
//                                                "This is automated system. Please contact card managers if this seems to be incorrect automation\n\n${inventory.getInvalidReason()}").queue(null) { _ -> }
//                                    }, { _ -> })
//                                }
                            }
                        }
                    } else {
                        collectorMonitor++
                    }

                    if (!test && backup == 360) {
                        backup = 1

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

                    if (!test) {
                        val g = client.getGuildById(CardData.guild)

                        if (g != null) {
                            val forum = g.getForumChannelById(CardData.tradingPlace)

                            if (forum != null) {
                                ArrayList(CardData.sessions).forEach { session ->
                                    val ch = forum.threadChannels.find { ch -> ch.idLong == session.postID }

                                    if (ch != null) {
                                        val timeStamp = Timestamp.valueOf(ch.timeCreated.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())

                                        if (currentTime - timeStamp.time >= CardData.TRADE_EXPIRATION_TIME) {
                                            session.expire(ch)
                                        }
                                    } else {
                                        session.expire()
                                    }
                                }
                            }
                        }
                    }

                    val jda = client.shards.firstOrNull()

                    CardData.auctionSessions.removeIf { auction ->
                        if (!auction.opened)
                            return@removeIf false

                        val ended = auction.autoClose && auction.autoCloseTime > 0L && auction.lastBidTime > 0L && currentTime >= auction.lastBidTime + auction.autoCloseTime

                        if (ended) {
                            val result = auction.closeSession(jda?.selfUser?.idLong ?: 0L, true)

                            val ch = auction.getAuctionChannel() ?: return@removeIf result.isEmpty()

                            if (result.isNotEmpty()) {
                                val mention = if (test) {
                                    "<@${StaticStore.MANDARIN_SMELL}>"
                                } else {
                                    "<@&${CardData.dealer}>"
                                }

                                ch.sendMessage("Tried to auto close the auction due to no bid for ${CardData.convertMillisecondsToText(auction.autoCloseTime)}, but failed\n\n$result\n\nPinging $mention for manual close").queue()

                                return@removeIf false
                            } else {
                                ch.sendMessage("Auction has been auto-closed due to no bid for ${CardData.convertMillisecondsToText(auction.autoCloseTime)}").queue()

                                val successMessage = StringBuilder()

                                if (auction.author != -1L)
                                    successMessage.append("<@").append(auction.author).append(">, ")

                                successMessage.append("<@").append(auction.getMostBidMember()).append("> Check your inventory")

                                ch.sendMessage(successMessage).queue()
                            }
                        }

                        return@removeIf ended
                    }

                    if (inviteLocked) {
                        val g = client.getGuildById(CardData.guild)

                        if (g != null && !g.isInvitesDisabled) {
                            g.manager.setInvitesDisabled(true).queue(null) { e ->
                                StaticStore.logger.uploadErrorLog(e, "E/CardBot::main - Failed to pause invite links")
                            }
                        }
                    }

                    val f = Runtime.getRuntime().freeMemory()
                    val t = Runtime.getRuntime().totalMemory()
                    val m = Runtime.getRuntime().maxMemory()

                    val percentage = 100.0 * (t - f) / m

                    if (percentage >= 90.0) {
                        StaticStore.logger.uploadLog("Warning : Memory is at danger, above 90% (${DataToString.df.format(percentage)}%)")
                    }

                    Logger.writeLog(Logger.BotInstance.CARD_DEALER)
                } catch(e: Exception) {
                    StaticStore.logger.uploadErrorLog(e, "CardBot::saver - Failed to perform background thread")
                }
            }
        }, 0, TimeUnit.MINUTES.toMillis(1))
    }

    override fun onGuildBan(event: GuildBanEvent) {
        val userID = event.user.idLong

        val inventory = CardData.inventories.remove(userID) ?: return
        CardData.notifierGroup.remove(userID)
        LogSession.gatherPreviousSessions(CardData.getUnixEpochTime(), -1).forEach { log ->
            log.optOut(userID)
            log.saveSessionAsFile()
        }

        val targetFile = inventory.extractAsFile()

        event.jda.retrieveUserById(StaticStore.MANDARIN_SMELL)
            .flatMap { u -> u.openPrivateChannel() }
            .flatMap { ch ->
                ch.sendMessage("Inventory of <@$userID> ($userID)")
                    .setFiles(FileUpload.fromData(targetFile, "inventory.json"))
            }.queue {
                event.jda.retrieveUserById(ServerData.get("gid"))
                    .flatMap { u -> u.openPrivateChannel() }
                    .flatMap { ch ->
                        ch.sendMessage("Inventory of <@$userID> ($userID)")
                            .setFiles(FileUpload.fromData(targetFile, "inventory.json"))
                    }.queue {
                        Files.deleteIfExists(targetFile.toPath())
                    }
            }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!ready)
            return

        if (event.isWebhookMessage)
            return

        val u = event.author

        if (u.idLong in CardData.optOut)
            return

        if (u.idLong in CardData.bannedUser)
            return

        val m = if (event.member == null) {
            val waiter = CountDownLatch(1)
            val atomicMember = AtomicReference<Member>(null)

            event.jda.getGuildById(CardData.guild)?.retrieveMemberById(u.id)?.queue({
                atomicMember.set(it)

                waiter.countDown()
            }) { _ ->
                waiter.countDown()
            }

            waiter.await()

            atomicMember.get()
        } else {
            event.member
        } ?: return

        val ch = event.channel

        val lastTime = CardData.lastMessageSent[u.id] ?: 0L
        val currentTime = CardData.getUnixEpochTime()

        if (currentTime - lastTime >= CardData.catFoodCooldown && ch.id !in CardData.excludedCatFoodChannel) {
            CardData.lastMessageSent[u.id] = currentTime

            val inventory = Inventory.getInventory(u.idLong)

            inventory.catFoods += if (CardData.minimumCatFoods != CardData.maximumCatFoods) {
                CardData.minimumCatFoods.rangeTo(CardData.maximumCatFoods).random()
            } else {
                CardData.maximumCatFoods
            }
        }

        if (CardData.isBanned(m))
            return

        val segments = event.message.contentRaw.lowercase().split(" ")

        if (locked && u.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val hub = StaticStore.getHolderHub(u.id)

        if (hub != null) {
            hub.componentHolder?.handleMessageDetected(event.message)
        }

        val firstSegment = if (segments.isEmpty())
            ""
        else
            segments[0]

        if (CardData.canPerformGlobalCommand(m, ch)) {
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
                "${globalPrefix}shardrank",
                "${globalPrefix}sr" -> {
                    ShardRank().execute(event)

                    return
                }
                "${globalPrefix}platinumshard",
                "${globalPrefix}ps" -> {
                    PlatinumShard().execute(event)

                    return
                }
            }
        }

        if (u.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m) && !CardData.isAllowed(ch))
            return

        when(firstSegment) {
            "${globalPrefix}save" -> Save().execute(event)
            "${globalPrefix}rollmanual",
            "${globalPrefix}rm" -> RollManual().execute(event)
            "${globalPrefix}trade",
            "${globalPrefix}tr" -> Trade().execute(event)
            "${globalPrefix}trademanual",
            "${globalPrefix}tm" -> TradeManual().execute(event)
            "${globalPrefix}cards",
            "${globalPrefix}card" -> Cards().execute(event)
            "${globalPrefix}purchase",
            "${globalPrefix}buy" -> Buy().execute(event)
            "${globalPrefix}roll" -> Roll().execute(event)
            "${globalPrefix}moidfyinventory",
            "${globalPrefix}modify",
            "${globalPrefix}mi"-> ModifyInventory().execute(event)
            "${globalPrefix}activate" -> Activate().execute(event)
            "${globalPrefix}equip" -> Equip().execute(event)
            "${globalPrefix}checkt0",
            "${globalPrefix}t0" -> Check(CardData.Tier.SPECIAL).execute(event)
            "${globalPrefix}checkt1",
            "${globalPrefix}t1" -> Check(CardData.Tier.COMMON).execute(event)
            "${globalPrefix}checkt2",
            "${globalPrefix}t2" -> Check(CardData.Tier.UNCOMMON).execute(event)
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
            "${globalPrefix}salvage",
            "${globalPrefix}sv" -> Salvage().execute(event)
            "${globalPrefix}lock" -> Lock().execute(event)
            "${globalPrefix}unlock" -> Unlock().execute(event)
            "${globalPrefix}craft",
            "${globalPrefix}cr" -> Craft().execute(event)
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
            "${globalPrefix}massshard",
            "${globalPrefix}mps" -> MassShard().execute(event)
            "${globalPrefix}salvagecost",
            "${globalPrefix}sc" -> SalvageCost().execute(event)
            "${globalPrefix}craftcost",
            "${globalPrefix}cc" -> CraftCost().execute(event)
            "${globalPrefix}hack" -> {
                if (test) {
                    Hack().execute(event)
                }
            }
            "${globalPrefix}managepack",
            "${globalPrefix}mp" -> ManagePack().execute(event)
            "${globalPrefix}logout",
            "${globalPrefix}lo" -> LogOut().execute(event)
            "${globalPrefix}resetcooldown",
            "${globalPrefix}rc" -> ResetCooldown().execute(event)
            "${globalPrefix}auctionplace",
            "${globalPrefix}ap" -> AuctionPlace().execute(event)
            "${globalPrefix}createauction",
            "${globalPrefix}cra" -> CreateAuction().execute(event)
            "${globalPrefix}addauctionplace",
            "${globalPrefix}aap" -> AddAuctionPlace().execute(event)
            "${globalPrefix}removeauctionplace",
            "${globalPrefix}rap" -> RemoveAuctionPlace().execute(event)
            "${globalPrefix}approveauction",
            "${globalPrefix}apa" -> ApproveAuction().execute(event)
            "${globalPrefix}cancelauction",
            "${globalPrefix}caa" -> CancelAuction().execute(event)
            "${globalPrefix}closeauction",
            "${globalPrefix}cla" -> CloseAuction().execute(event)
            "${globalPrefix}changeauctiontime",
            "${globalPrefix}cat" -> ChangeAuctionTime().execute(event)
            "${globalPrefix}bid" -> Bid().execute(event)
            "${globalPrefix}cancelbid",
            "${globalPrefix}cb" -> CancelBid().execute(event)
            "${globalPrefix}forcecancelbid",
            "${globalPrefix}fcb" -> ForceCancelBid().execute(event)
            "${globalPrefix}test" -> Test().execute(event)
            "${globalPrefix}pauseinvite",
            "${globalPrefix}pi" -> PauseInvite().execute(event)
            "${globalPrefix}manageslot",
            "${globalPrefix}ms" -> ManageSlot().execute(event)
            "${globalPrefix}registeremojiserver",
            "${globalPrefix}res" -> RegisterEmojiServer().execute(event)
            "${globalPrefix}unregisteremojiserver",
            "${globalPrefix}ues" -> UnregisterEmojiServer().execute(event)
            "${globalPrefix}slotmachine",
            "${globalPrefix}slot",
            "${globalPrefix}slots",
            "${globalPrefix}sl" -> Slot().execute(event)
            "${globalPrefix}transferinventory",
            "${globalPrefix}ti" -> TransferInventory().execute(event)
            "${globalPrefix}listguild",
            "${globalPrefix}lig" -> ListGuild().execute(event)
            "${globalPrefix}leaveguild",
            "${globalPrefix}leg" -> LeaveGuild().execute(event)
            "${globalPrefix}banuser",
            "${globalPrefix}bu" -> BanUser().execute(event)
            "${globalPrefix}managecardskin",
            "${globalPrefix}mc" -> ManageCardSkin().execute(event)
            "${globalPrefix}buyskin",
            "${globalPrefix}bs" -> BuySkin().execute(event)
            "${globalPrefix}optout" -> OptOut().execute(event)
            "${globalPrefix}wipeinventory",
            "${globalPrefix}wi" -> WipeInventory().execute(event)
            "${globalPrefix}injectinventory",
            "${globalPrefix}ii" -> InjectInventory().execute(event)
            "${globalPrefix}ejectinventory",
            "${globalPrefix}ei" -> EjectInventory().execute(event)
            "${globalPrefix}memory",
            "${globalPrefix}mm" -> Memory().execute(event)
            "${globalPrefix}extractjson",
            "${globalPrefix}ej" -> ExtractJson().execute(event)
        }

        val session = CardData.sessions.find { s -> s.postID == event.channel.idLong }

        if (session != null) {
            when(firstSegment) {
                "${globalPrefix}suggest",
                "${globalPrefix}su" -> Suggest(session).execute(event)
                "${globalPrefix}confirm" -> Confirm(session).execute(event)
                "${globalPrefix}cancel" -> Cancel(session).execute(event)
            }

            return
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        super.onMessageDelete(event)

        CardData.skins.forEach { skin ->
            if (skin.messageID == event.messageIdLong) {
                skin.messageID = -1L
                skin.cacheLink = ""
            }
        }

        StaticStore.holders.values.forEach { hub -> hub.handleMessageDelete(event.messageId) }
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (event.message.author.idLong != event.jda.selfUser.idLong)
            return

        StaticStore.holders.values.forEach { hub ->
            hub.messageHolder?.handleMessageUpdated(event.message)
            hub.componentHolder?.handleMessageUpdated(event.message)
            hub.modalHolder?.handleMessageUpdated(event.message)
        }
    }

    override fun onEmojiAdded(event: EmojiAddedEvent) {
        super.onEmojiAdded(event)

        val g = event.guild

        if (g.idLong !in SlotEmojiContainer.registeredServer)
            return

        SlotEmojiContainer.updateEmojiAdd(event.emoji)
    }

    override fun onEmojiRemoved(event: EmojiRemovedEvent) {
        super.onEmojiRemoved(event)

        val g = event.guild

        if (g.idLong !in SlotEmojiContainer.registeredServer)
            return

        SlotEmojiContainer.updateEmojiRemoved(event.emoji)

        saveCardData()
    }

    override fun onGenericInteractionCreate(event: GenericInteractionCreateEvent) {
        super.onGenericInteractionCreate(event)

        val u = event.user

        if (u.idLong in CardData.optOut)
            return

        if (u.idLong in CardData.bannedUser)
            return

        val waiter = CountDownLatch(1)
        val atomicMember = AtomicReference<Member>(null)

        event.jda.getGuildById(CardData.guild)?.retrieveMemberById(u.id)?.queue({
            atomicMember.set(it)

            waiter.countDown()
        }) { _ ->
            waiter.countDown()
        }

        waiter.await()

        val m = atomicMember.get() ?: return

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
        SlotEmojiContainer.load(event.jda.shardManager)

        // Load already cached-links only
        CardData.skins.forEach { s -> s.cache(event.jda, false) }

        CardData.slotMachines.forEach { s -> s.content.forEach { c -> c.load() } }

        TransactionLogger.logChannel = event.jda.getGuildChannelById(CardData.transactionLog) as MessageChannel
        TransactionLogger.tradeChannel = event.jda.getGuildChannelById(CardData.tradingLog) as MessageChannel
        TransactionLogger.modChannel = event.jda.getGuildChannelById(CardData.modLog) as MessageChannel
        TransactionLogger.catFoodChannel = event.jda.getGuildChannelById(CardData.catFoodLog) as MessageChannel
        TransactionLogger.bidLogChannel = event.jda.getGuildChannelById(CardData.bidLog) as MessageChannel
        TransactionLogger.slotChannel = event.jda.getGuildChannelById(CardData.slotLog) as MessageChannel

        StaticStore.loggingChannel = ServerData.get("loggingChannel")

        val manager = event.jda.shardManager

        if (manager != null) {
            StaticStore.logger.assignClient(manager)
        }

        ready = true

        val wasSafe = StaticStore.safeClose
        StaticStore.safeClose = false

        CardData.auctionSessions.forEach { it.queueSession(event.jda) }

        val g = event.jda.getGuildById(CardData.guild) ?: return

        g.retrieveBanList().queue { list ->
            list.forEach { ban ->
                val userID = ban.user.idLong

                val inventory = CardData.inventories[userID] ?: return@forEach

                val countdown = CountDownLatch(1)

                g.retrieveMember(UserSnowflake.fromId(userID)).queue({ member ->
                    StaticStore.logger.uploadLog("W/CardBot::onReady - Unbanned member is found in banned user list? <@$userID> ($userID)")

                    countdown.countDown()
                }) { _ ->
                    CardData.inventories.remove(userID)
                    CardData.notifierGroup.remove(userID)
                    LogSession.gatherPreviousSessions(CardData.getUnixEpochTime(), -1).forEach { log ->
                        log.optOut(userID)
                        log.saveSessionAsFile()
                    }

                    val file = inventory.extractAsFile()

                    event.jda.retrieveUserById(StaticStore.MANDARIN_SMELL)
                        .flatMap { u -> u.openPrivateChannel() }
                        .flatMap { ch ->
                            ch.sendMessage("Inventory of <@$userID> ($userID)")
                                .setFiles(FileUpload.fromData(file, "inventory.json"))
                        }.queue({
                            if (!test) {
                                event.jda.retrieveUserById(ServerData.get("gid"))
                                    .flatMap { u -> u.openPrivateChannel() }
                                    .flatMap { ch ->
                                        ch.sendMessage("Inventory of <@$userID> ($userID)")
                                            .setFiles(FileUpload.fromData(file, "inventory.json"))
                                    }.queue({
                                        Files.deleteIfExists(file.toPath())
                                        countdown.countDown()
                                    }) { e ->
                                        StaticStore.logger.uploadErrorLog(e, "E/CardBot::onReady - Failed to send extracted inventory")

                                        countdown.countDown()
                                    }
                            } else {
                                countdown.countDown()
                            }
                        }) { e ->
                            StaticStore.logger.uploadErrorLog(e, "E/CardBot::onReady - Failed to send extracted inventory")

                            countdown.countDown()
                        }
                }

                countdown.await()
            }
        }

        if (test)
            return

        val ch = g.getGuildChannelById(CardData.statusChannel) ?: return

        if (ch is GuildMessageChannel) {
            if (wasSafe) {
                ch.sendMessage("${event.jda.selfUser.asMention} is online now").queue()
            } else {
                ch.sendMessage("There was technical issue, or unknown problem that made bot go offline. ${event.jda.selfUser.asMention} is online now").queue()
            }
        }

        saveCardData()
    }

    override fun onShutdown(event: ShutdownEvent) {
        StaticStore.logger.uploadErrorLog(Exception("Shutdown"), "I/CardBot::onShutdown - Shutting down")
        Logger.writeLog(Logger.BotInstance.CARD_DEALER)
    }

    private fun initialize() {
        CommonStatic.ctx = PackContext()
        CommonStatic.getConfig().ref = false
        CommonStatic.getConfig().updateOldMusic = false

        LangID.initialize()

        readCardData()

        Initializer.checkAssetDownload(false)

        StaticStore.logCommand = true
    }

    private fun readCardData() {
        val cardFolder = File("./data/cards")

        if (!cardFolder.exists())
            return

        val tiers = cardFolder.listFiles() ?: return

        for(t in tiers) {
            if (t.name == "Skin")
                continue

            val cards = t.listFiles() ?: continue

            cards.sortBy {
                if (it.name.startsWith("-")) {
                    -it.name.split("-")[1].toInt()
                } else {
                    it.name.split("-")[0].toInt()
                }
            }

            for(card in cards) {
                val tier = when(t.name) {
                    "Tier 0" -> CardData.Tier.SPECIAL
                    "Tier 1" -> CardData.Tier.COMMON
                    "Tier 2" -> CardData.Tier.UNCOMMON
                    "Tier 3" -> CardData.Tier.ULTRA
                    "Tier 4" -> CardData.Tier.LEGEND
                    else -> throw IllegalStateException("Invalid tier type ${t.name} found")
                }

                val fileName = card.name.replace(".png", "")

                val nameData = if (fileName.startsWith("-")) {
                    val tempData = fileName.split(Regex("-"), 3)

                    if (tempData.size != 3)
                        throw IllegalStateException("Invalid card name format : $fileName")

                    listOf("-" + tempData[1], tempData[2])
                } else {
                    fileName.split(Regex("-"), 2)
                }

                val cardID = if (tier == CardData.Tier.SPECIAL) {
                    -(nameData[0].toInt())
                } else {
                    nameData[0].toInt()
                }

                CardData.cards.add(Card(cardID, tier, nameData[1], card))
            }
        }

        CardData.cards.removeIf { c ->
            if (c.tier == CardData.Tier.SPECIAL)
                return@removeIf false

            val result = !CardData.bannerData.any { t -> t.any { b -> c.tier == CardData.Tier.LEGEND || c.unitID in b } }

            if (result) {
                println("Removing ${c.unitID}")
            }

            result
        }

        CardData.special.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.SPECIAL })
        CardData.common.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.COMMON }.filter { r -> CardData.permanents[r.tier.ordinal].any { i -> r.unitID in CardData.bannerData[r.tier.ordinal][i] } })
        CardData.uncommon.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.UNCOMMON }.filter { r -> CardData.permanents[r.tier.ordinal].any { i -> r.unitID in CardData.bannerData[r.tier.ordinal][i] } })
        CardData.ultraRare.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.ULTRA }.filter { r -> CardData.permanents[r.tier.ordinal].any { i -> r.unitID in CardData.bannerData[r.tier.ordinal][i] } })
        CardData.legendRare.addAll(CardData.cards.filter { r -> r.tier == CardData.Tier.LEGEND }.filter { r -> CardData.bannerData[r.tier.ordinal].any { arr -> r.unitID !in arr } })

        val serverElement: JsonElement? = StaticStore.getJsonFile(if (test) "testserverinfo" else "serverinfo")

        if (serverElement != null && serverElement.isJsonObject) {
            val serverInfo = serverElement.asJsonObject

            if (serverInfo.has("lang")) {
                StaticStore.langs = StaticStore.jsonToMapString(serverInfo.getAsJsonArray("lang"))
            }
        }

        val element: JsonElement? = StaticStore.getJsonFile(if (test) "testCardSave" else "cardSave")

        if (element == null || !element.isJsonObject)
            return

        val obj = element.asJsonObject

        if (obj.has("skins")) {
            val arr = obj.getAsJsonArray("skins")

            arr.forEach { e ->
                val skin = Skin.fromJson(e.asJsonObject) ?: return@forEach

                CardData.skins.add(skin)
            }
        }

        if (obj.has("locked")) {
            locked = obj.get("locked").asBoolean
        }

        if (obj.has("rollLocked")) {
            rollLocked = obj.get("rollLocked").asBoolean
        }

        if (obj.has("cardPacks")) {
            val arr = obj.getAsJsonArray("cardPacks")

            arr.forEach { e ->
                CardData.cardPacks.add(CardPack.fromJson(e.asJsonObject))
            }
        }

        if (obj.has("slotMachines")) {
            val arr = obj.getAsJsonArray("slotMachines")

            arr.forEach { e ->
                CardData.slotMachines.add(SlotMachine.fromJson(e.asJsonObject))
            }
        }

        if (obj.has("inventory")) {
            val arr = obj.getAsJsonArray("inventory")

            for (i in 0 until arr.size()) {
                val pair = arr[i].asJsonObject

                if (!pair.has("key") || !pair.has("val"))
                    continue

                val keyData = pair["key"]

                if (keyData !is JsonPrimitive)
                    continue

                val key = if (keyData.isString) {
                    keyData.asString.toLong()
                } else {
                    keyData.asLong
                }

                val value = Inventory.readInventory(key, pair["val"].asJsonObject)

                CardData.inventories[key] = value
            }
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
                if (e is JsonPrimitive && e.isString) {
                    val text = e.asString

                    if (StaticStore.isNumeric(text)) {
                        val id = StaticStore.safeParseLong(text)

                        CardData.notifierGroup[id] = booleanArrayOf(true, false)
                    }
                } else if (e is JsonObject && e.has("key") && e.has("val")) {
                    val id = e.get("key").asLong
                    val array = e.getAsJsonArray("val").map { e -> e.asBoolean }.toBooleanArray()

                    CardData.notifierGroup[id] = array
                }
            }
        }

        if (obj.has("cooldown")) {
            obj.getAsJsonArray("cooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val value = o.getAsJsonArray("val")

                    val packCooldownMap = HashMap<String, Long>()

                    value.forEach { e ->
                        if (e.isJsonObject) {
                            val mapObj = e.asJsonObject

                            if (mapObj.has("key") && mapObj.has("val")) {
                                val uuid = mapObj.get("key").asString

                                val pack = CardData.cardPacks.find { pack -> pack.uuid == uuid }

                                if (pack != null) {
                                    packCooldownMap[uuid] = mapObj.get("val").asLong
                                }
                            }
                        }
                    }

                     val key = o.get("key")

                    if (key !is JsonPrimitive)
                        return@forEach

                    if (key.isString) {
                        CardData.cooldown[StaticStore.safeParseLong(key.asString)] = packCooldownMap
                    } else if (key.isNumber) {
                        CardData.cooldown[key.asLong] = packCooldownMap
                    }
                }
            }
        }

        if (obj.has("slotCooldown")) {
            obj.getAsJsonArray("slotCooldown").forEach {
                val o = it.asJsonObject

                if (o.has("key") && o.has("val")) {
                    val value = o.getAsJsonArray("val")

                    val slotCooldownMap = HashMap<String, Long>()

                    value.forEach { e ->
                        if (e.isJsonObject) {
                            val mapObj = e.asJsonObject

                            if (mapObj.has("key") && mapObj.has("val")) {
                                val uuid = mapObj.get("key").asString

                                val pack = CardData.slotMachines.find { slot -> slot.uuid == uuid }

                                if (pack != null) {
                                    slotCooldownMap[uuid] = mapObj.get("val").asLong
                                }
                            }
                        }
                    }

                    val key = o.get("key")

                    if (key !is JsonPrimitive)
                        return@forEach

                    if (key.isString) {
                        CardData.slotCooldown[StaticStore.safeParseLong(key.asString)] = slotCooldownMap
                    } else if (key.isNumber) {
                        CardData.slotCooldown[key.asLong] = slotCooldownMap
                    }
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

        if (obj.has("salvageCost")) {
            val arr = obj.getAsJsonArray("salvageCost")

            arr.forEach { e ->
                val o = e.asJsonObject

                if (o.has("key") && o.has("cost")) {
                    val cost = o.get("cost").asInt

                    when(o.get("key").asString) {
                        "t1" -> CardData.SalvageMode.T1
                        "t2" -> CardData.SalvageMode.T2
                        "seasonal" -> CardData.SalvageMode.SEASONAL
                        "collab" -> CardData.SalvageMode.COLLAB
                        "t3" -> CardData.SalvageMode.T3
                        "t4" -> CardData.SalvageMode.T4
                        else -> throw IllegalStateException("E/CardBot::readCardData - Unknown salvage cost type : ${o.get("key")}")
                    }.cost = cost
                }
            }
        }

        if (obj.has("craftCost")) {
            val arr = obj.getAsJsonArray("craftCost")

            arr.forEach { e ->
                val o = e.asJsonObject

                if (o.has("key") && o.has("cost")) {
                    val cost = o.get("cost").asInt

                    when(o.get("key").asString) {
                        "t2" -> CardData.CraftMode.T2
                        "seasonal" -> CardData.CraftMode.SEASONAL
                        "collab" -> CardData.CraftMode.COLLAB
                        "t3" -> CardData.CraftMode.T3
                        "t4" -> CardData.CraftMode.T4
                        else -> throw IllegalStateException("E/CardBot::readCardData - Unknown craft type : ${o.get("key")}")
                    }.cost = cost
                }
            }
        }

        if (obj.has("safeClose")) {
            StaticStore.safeClose = obj.get("safeClose").asBoolean
        }

        if (obj.has("auctionPlaces")) {
            val arr = obj.getAsJsonArray("auctionPlaces")

            arr.forEach { e ->
                CardData.auctionPlaces.add(e.asLong)
            }
        }

        if (obj.has("auctionSessions")) {
            val arr = obj.getAsJsonArray("auctionSessions")

            arr.forEach { e ->
                CardData.auctionSessions.add(AuctionSession.fromJson(e.asJsonObject))
            }
        }

        if (obj.has("auctionSessionNumber")) {
            CardData.auctionSessionNumber = obj.get("auctionSessionNumber").asLong
        }

        if (obj.has("slotEmojiContainer")) {
            SlotEmojiContainer.fromJson(obj.getAsJsonArray("slotEmojiContainer"))
        }

        if (obj.has("inviteLocked")) {
            inviteLocked = obj.get("inviteLocked").asBoolean
        }

        if (obj.has("optOut")) {
            obj.getAsJsonArray("optOut").forEach { e ->
                CardData.optOut.add(e.asLong)
            }
        }

        if (obj.has("purchaseNotifier")) {
            obj.getAsJsonArray("purchaseNotifier").forEach { e ->
                CardData.purchaseNotifier.add(e.asLong)
            }
        }
    }

    @Synchronized
    fun saveCardData() {
        val obj = JsonObject()

        obj.addProperty("locked", locked)
        obj.addProperty("rollLocked", rollLocked)

        val cardPacks = JsonArray()

        CardData.cardPacks.forEach { pack ->
            cardPacks.add(pack.asJson())
        }

        obj.add("cardPacks", cardPacks)

        val slotMachines = JsonArray()

        CardData.slotMachines.forEach { machine ->
            slotMachines.add(machine.asJson())
        }

        obj.add("slotMachines", slotMachines)

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
            val cooldownMap = CardData.cooldown[it]

            if (cooldownMap != null) {
                val o = JsonObject()

                o.addProperty("key", it)

                val value = JsonArray()

                cooldownMap.forEach { (uuid, cd) ->
                    val mapObj = JsonObject()

                    mapObj.addProperty("key", uuid)
                    mapObj.addProperty("val", cd)

                    value.add(mapObj)
                }

                o.add("val", value)

                cooldown.add(o)
            }
        }

        obj.add("cooldown", cooldown)

        val slotCooldown = JsonArray()

        CardData.slotCooldown.keys.forEach {
            val cooldownMap = CardData.slotCooldown[it]

            if (cooldownMap != null) {
                val o = JsonObject()

                o.addProperty("key", it)

                val value = JsonArray()

                cooldownMap.forEach { (uuid, cd) ->
                    val mapObj = JsonObject()

                    mapObj.addProperty("key", uuid)
                    mapObj.addProperty("val", cd)

                    value.add(mapObj)
                }

                o.add("val", value)

                slotCooldown.add(o)
            }
        }

        obj.add("slotCooldown", slotCooldown)

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

        CardData.notifierGroup.forEach { (id, notifier) ->
            val o = JsonObject()

            o.addProperty("key", id)

            val a = JsonArray()

            notifier.forEach { b -> a.add(b) }

            o.add("val", a)

            notifierGroup.add(o)
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

        val salvageCost = JsonArray()

        CardData.SalvageMode.entries.forEach { mode ->
            val key = when(mode) {
                CardData.SalvageMode.T1 -> "t1"
                CardData.SalvageMode.T2 -> "t2"
                CardData.SalvageMode.SEASONAL -> "seasonal"
                CardData.SalvageMode.COLLAB -> "collab"
                CardData.SalvageMode.T3 -> "t3"
                CardData.SalvageMode.T4 -> "t4"
            }

            val o = JsonObject()

            o.addProperty("key", key)
            o.addProperty("cost", mode.cost)

            salvageCost.add(o)
        }

        obj.add("salvageCost", salvageCost)

        val craftCost = JsonArray()

        CardData.CraftMode.entries.forEach { mode ->
            val key = when(mode) {
                CardData.CraftMode.T2 -> "t2"
                CardData.CraftMode.SEASONAL -> "seasonal"
                CardData.CraftMode.COLLAB -> "collab"
                CardData.CraftMode.T3 -> "t3"
                CardData.CraftMode.T4 -> "t4"
            }

            val o = JsonObject()

            o.addProperty("key", key)
            o.addProperty("cost", mode.cost)

            craftCost.add(o)
        }

        obj.add("craftCost", craftCost)

        obj.addProperty("safeClose", StaticStore.safeClose)

        val auctionPlaces = JsonArray()

        CardData.auctionPlaces.forEach { id -> auctionPlaces.add(id) }

        obj.add("auctionPlaces", auctionPlaces)

        val auctionSessions = JsonArray()

        CardData.auctionSessions.forEach { s -> auctionSessions.add(s.asJson()) }

        obj.add("auctionSessions", auctionSessions)

        obj.addProperty("auctionSessionNumber", CardData.auctionSessionNumber)

        obj.add("slotEmojiContainer", SlotEmojiContainer.asJson())

        obj.addProperty("inviteLocked", inviteLocked)

        val skins = JsonArray()

        CardData.skins.forEach { skin ->
            skins.add(skin.asJson())
        }

        obj.add("skins", skins)

        val optOut = JsonArray()

        CardData.optOut.forEach { id ->
            optOut.add(id)
        }

        obj.add("optOut", optOut)

        val purchaseNotifier = JsonArray()

        CardData.purchaseNotifier.forEach { id ->
            purchaseNotifier.add(id)
        }

        obj.add("purchaseNotifier", purchaseNotifier)

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

            val writer = FileWriter("./data/${if (test) "testCardSave" else "cardSave"}.json")

            writer.append(mapper.writeValueAsString(tree))
            writer.close()
        } catch (e: IOException) {
            StaticStore.logger.uploadErrorLog(e, "Failed to save card save")
        }
    }
}
