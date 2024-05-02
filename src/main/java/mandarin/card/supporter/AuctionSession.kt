package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ScheduledFuture
import kotlin.math.max

class AuctionSession(
    val id: Long,
    val channel: Long,
    val author: Long,
    val card: Card,
    val amount: Int,
    var endDate: Long,
    val anonymous: Boolean,
    val initialPrice: Long,
    val minimumBid: Long
) {
    companion object {
        fun fromJson(obj: JsonObject) : AuctionSession {
            if (!StaticStore.hasAllTag(obj, "id", "channel", "author", "card", "amount", "endDate", "anonymous", "currentBid", "bidData", "opened", "message", "minimumBid"))
                throw IllegalStateException("E/AuctionSession::fromJson - Invalid json object was tried to be loaded!")

            val id = obj.get("id").asLong
            val channel = obj.get("channel").asLong
            val author = obj.get("author").asLong

            val cardId = obj.get("card").asInt
            val card = CardData.cards.find { c -> c.unitID == cardId } ?: throw IllegalStateException("E/AuctionSession::fromJson - Failed to find card with ID of $cardId")

            val amount = obj.get("amount").asInt
            val endDate = obj.get("endDate").asLong
            val anonymous = obj.get("anonymous").asBoolean
            val currentBid = obj.get("currentBid").asLong
            val minimumBid = obj.get("minimumBid").asLong

            val opened = obj.get("opened").asBoolean

            val session = AuctionSession(id, channel, author, card, amount, endDate, anonymous, currentBid, minimumBid)

            val bidData = obj.getAsJsonArray("bidData")

            bidData.forEach { e ->
                val o = e.asJsonObject

                if (StaticStore.hasAllTag(o, "key", "val")) {
                    session.bid(o.get("key").asLong, o.get("val").asLong)
                }
            }

            session.opened = opened

            session.message = obj.get("message").asLong

            return session
        }
    }

    val bidData = HashMap<Long, Long>()

    val currentBid: Long
        get() {
            return if (bidData.isEmpty()) {
                initialPrice
            } else {
                bidData.maxOf { (_, amount) -> amount }
            }
        }

    var opened = true
    var message = -1L

    private lateinit var auctionChannel: GuildMessageChannel
    private lateinit var auctionMessage: Message

    private var scheduler: ScheduledFuture<*>? = null

    fun bid(userId: Long, amount: Long) {
        bidData[userId] = max((bidData[userId] ?: 0L), amount)

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        TransactionLogger.logBid(this, userId, amount)
    }

    fun cancelBid(userID: Long) {
        val previousAmount = bidData[userID] ?: 0L

        bidData.remove(userID)

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        TransactionLogger.logBidCancel(this, userID, previousAmount)
    }

    fun forceCancelBid(canceler: Long, userID: Long) {
        val previousAmount = bidData[userID] ?: 0L

        bidData.remove(userID)

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        TransactionLogger.logBidForceCancel(this, canceler, userID, previousAmount)
    }

    fun queueSession(client: JDA) {
        load(client, false)

        // No point to handle this auction if channel itself doesn't exist
        if (!this::auctionChannel.isInitialized) {
            StaticStore.logger.uploadLog("W/AuctionSession::queueSession - Failed to load auction channel $channel")

            return
        }

        if (endDate * 1000L < CardData.getUnixEpochTime()) {
            //Unhandled auction close
            if (opened) {
                opened = false

                if (getMostBidMember() == 0L) {
                    val builder = StringBuilder("There was no member who participated in this auction. Auto-canceling the auction...")

                    if (author != -1L) {
                        val inventory = Inventory.getInventory(author)

                        inventory.auctionQueued[card] = (inventory.auctionQueued[card] ?: 0) - amount
                        inventory.cards[card] = (inventory.cards[card] ?: 0) + amount

                        builder.append("\n\nPinging author <@$author> to notify that auction has been canceled due to no participants")
                    }

                    CardData.auctionSessions.remove(this)

                    CardBot.saveCardData()

                    auctionChannel.sendMessage(builder.toString()).queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo("Canceled"))
                            .setAllowedMentions(ArrayList())
                            .queue()

                        if (auctionMessage.isPinned)
                            auctionMessage.unpin().queue()
                    }
                } else {
                    auctionChannel.sendMessage("Auction #$id has ended! Waiting for approval of <@${StaticStore.MANDARIN_SMELL}>!").queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo())
                            .setAllowedMentions(ArrayList())
                            .queue()
                    }
                }
            }

            return
        }

        //Unstarted auction
        if (message == -1L) {
            auctionChannel.sendMessage(getAuctionInfo()).setAllowedMentions(ArrayList()).queue { msg ->
                message = msg.idLong
                auctionMessage = msg

                CardBot.saveCardData()

                msg.pin().queue {
                    load(client, true)
                }
            }
        }

        //Resuming auction
        scheduler = StaticStore.executorHandler.postDelayed(endDate * 1000L - CardData.getUnixEpochTime()) {
            if (opened) {
                opened = false

                if (getMostBidMember() == 0L) {
                    val builder = StringBuilder("There was no member who participated in this auction. Auto-canceling the auction...")

                    if (author != -1L) {
                        val inventory = Inventory.getInventory(author)

                        inventory.auctionQueued[card] = (inventory.auctionQueued[card] ?: 0) - amount
                        inventory.cards[card] = (inventory.cards[card] ?: 0) + amount

                        builder.append("\n\nPinging author <@$author> to notify that auction has been canceled due to no participants")
                    }

                    CardData.auctionSessions.remove(this)

                    CardBot.saveCardData()

                    auctionChannel.sendMessage(builder.toString()).queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo("Canceled"))
                            .setAllowedMentions(ArrayList())
                            .queue()

                        if (auctionMessage.isPinned)
                            auctionMessage.unpin().queue()
                    }
                } else {
                    auctionChannel.sendMessage("Auction $id has ended! Waiting for approval of <@${StaticStore.MANDARIN_SMELL}>!").queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo())
                            .setAllowedMentions(ArrayList())
                            .queue()
                    }
                }
            }
        }
    }

    fun cancelSession(managerID: Long) {
        if (author != -1L) {
            val inventory = Inventory.getInventory(author)

            inventory.auctionQueued[card] = (inventory.auctionQueued[card] ?: 0) - amount
            inventory.cards[card] = (inventory.cards[card] ?: 0) + amount
        }

        CardData.auctionSessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logAuctionCancel(managerID, this)

        opened = false

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo("Canceled"))
                .setAllowedMentions(ArrayList())
                .queue()

            if (auctionMessage.isPinned)
                auctionMessage.unpin().queue()
        }
    }

    fun closeSession(managerID: Long) : String {
        val memberId = getMostBidMember()

        if (memberId == 0L) {
            return "Auction performing failed\n\nReason : Failed to find most bid user"
        }

        val bidCf = bidData[memberId] ?: return "Auction performing failed\n\nReason : Failed to get bid data from user <@$memberId> [$memberId]"

        val inventory = Inventory.getInventory(memberId)

        inventory.catFoods -= bidCf
        inventory.cards[card] = (inventory.cards[card] ?: 0) + amount

        if (author != -1L) {
            val authorInventory = Inventory.getInventory(author)

            authorInventory.catFoods += bidCf
            authorInventory.auctionQueued[card] = (authorInventory.auctionQueued[card] ?: 0) - amount
        }

        CardData.auctionSessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logAuctionClose(managerID, this)
        TransactionLogger.logAuctionResult(this)

        opened = false

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo("Force Closed"))
                .setAllowedMentions(ArrayList())
                .queue()

            if (auctionMessage.isPinned)
                auctionMessage.unpin().queue()
        }

        return ""
    }

    fun performAuction(managerID: Long) : String {
        val memberId = getMostBidMember()

        if (memberId == 0L) {
            return "Auction performing failed\n\nReason : Failed to find most bid user"
        }

        val bidCf = bidData[memberId] ?: return "Auction performing failed\n\nReason : Failed to get bid data from user <@$memberId> [$memberId]"

        val inventory = Inventory.getInventory(memberId)

        inventory.catFoods -= bidCf
        inventory.cards[card] = (inventory.cards[card] ?: 0) + amount

        if (author != -1L) {
            val authorInventory = Inventory.getInventory(author)

            authorInventory.catFoods += bidCf
            authorInventory.auctionQueued[card] = (authorInventory.auctionQueued[card] ?: 0) - amount
        }

        CardData.auctionSessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logAuctionApprove(managerID, this)
        TransactionLogger.logAuctionResult(this)

        opened = false

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo("Approved"))
                .setAllowedMentions(ArrayList())
                .queue()

            if (auctionMessage.isPinned)
                auctionMessage.unpin().queue()
        }

        return ""
    }

    fun changeEndTime(managerID: Long, newTime: Long) {
        if (scheduler?.isDone == false) {
            scheduler?.cancel(true)
        }

        val oldTime = endDate

        endDate = newTime

        TransactionLogger.logAuctionEndDateChange(managerID, oldTime, this)

        opened = true

        if (this::auctionMessage.isInitialized) {
            auctionMessage
                .editMessage(getAuctionInfo())
                .setAllowedMentions(ArrayList())
                .queue()
        }

        scheduler = StaticStore.executorHandler.postDelayed(endDate * 1000L - CardData.getUnixEpochTime()) {
            if (opened) {
                opened = false

                if (getMostBidMember() == 0L) {
                    val builder = StringBuilder("There was no member who participated in this auction. Auto-canceling the auction...")

                    if (author != -1L) {
                        val inventory = Inventory.getInventory(author)

                        inventory.auctionQueued[card] = (inventory.auctionQueued[card] ?: 0) - amount
                        inventory.cards[card] = (inventory.cards[card] ?: 0) + amount

                        builder.append("\n\nPinging author <@$author> to notify that auction has been canceled due to no participants")
                    }

                    CardData.auctionSessions.remove(this)

                    CardBot.saveCardData()

                    auctionChannel.sendMessage(builder.toString()).queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo("Canceled"))
                            .setAllowedMentions(ArrayList())
                            .queue()

                        if (auctionMessage.isPinned)
                            auctionMessage.unpin().queue()
                    }
                } else {
                    auctionChannel.sendMessage("Auction $id has ended! Waiting for approval of <@${StaticStore.MANDARIN_SMELL}>!").queue()

                    if (this::auctionMessage.isInitialized) {
                        auctionMessage
                            .editMessage(getAuctionInfo())
                            .setAllowedMentions(ArrayList())
                            .queue()
                    }
                }
            }
        }
    }

    fun getMostBidMember() : Long {
        var member = 0L
        var bid = 0L

        bidData.keys.forEach { id ->
            val userBid = bidData[id] ?: 0L

            if (userBid > bid) {
                bid = userBid
                member = id
            }
        }

        return member
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("id", id)
        obj.addProperty("channel", channel)
        obj.addProperty("author", author)
        obj.addProperty("card", card.unitID)
        obj.addProperty("amount", amount)
        obj.addProperty("endDate", endDate)
        obj.addProperty("anonymous", anonymous)
        obj.addProperty("currentBid", currentBid)
        obj.addProperty("minimumBid", minimumBid)
        obj.addProperty("opened", opened)
        obj.addProperty("message", message)

        val bidArray = JsonArray()

        bidData.forEach { (userId, cf) ->
            val o = JsonObject()

            o.addProperty("key", userId)
            o.addProperty("val", cf)

            bidArray.add(o)
        }

        obj.add("bidData", bidArray)

        return obj
    }

    fun getAuctionMessage() : Message? {
        return if (!this::auctionMessage.isInitialized)
            null
        else
            auctionMessage
    }

    private fun getAuctionInfo(status: String = "") : String {
        val builder = StringBuilder("## Auction #$id\n")

        builder.append(getAuthor()).append(" has opened the acution\n")
            .append("\nCard : ")
            .append(card.cardInfo())

        if (amount >= 2) {
            builder.append("x$amount")
        }

        builder.append("\nEnd Date : <t:")
            .append(endDate)
            .append(":f> (<t:")
            .append(endDate)
            .append(":R>)")
            .append("\n\n### Bid Info\nMost Bid User : ")
            .append(getBidMember())
            .append("\nAmount of ")
            .append(EmojiStore.ABILITY["CF"]?.formatted)
            .append(" : ")
            .append(currentBid)
            .append("\nMinimum Bid Increase : ")
            .append(EmojiStore.ABILITY["CF"]?.formatted)
            .append(minimumBid)
            .append("\n\nStatus : **")

        if (opened) {
            builder.append("Opened")
        } else {
            if (status.isEmpty()) {
                builder.append("Closed [Waiting for Approval]")
            } else {
                builder.append(status)
            }
        }

        builder.append("**")

        if (anonymous) {
            builder.append("\n\nThis is an anonymous auction. You have to bid in DM! Channel ID is $channel")
        }

        return builder.toString()
    }

    private fun getBidMember() : String {
        if (anonymous)
            return "Anonymous"

        if (bidData.isEmpty())
            return "None"

        return "<@${getMostBidMember()}>"
    }

    private fun getAuthor() : String {
        return if (author == -1L)
            "System"
        else
            "User <@$author>"
    }

    private fun load(client: JDA, force: Boolean) {
        if (!force && this::auctionChannel.isInitialized && this::auctionMessage.isInitialized)
            return

        val g = client.getGuildById(CardData.guild)

        if (g == null) {
            StaticStore.logger.uploadLog("W/AuctionSession::load - Failed to get guild data => ${CardData.guild}")

            return
        }

        val ch = g.getGuildChannelById(channel)

        if (ch == null) {
            StaticStore.logger.uploadLog("W/AuctionSession::load - Channel doesn't exist => $channel")

            return
        }

        if (ch !is GuildMessageChannel) {
            StaticStore.logger.uploadLog("W/AuctionSession::load - Loaded channel isn't GuildMessageChannel => $channel")

            return
        }

        auctionChannel = ch

        if (message != -1L) {
            val waiter = CountDownLatch(1)

            ch.retrieveMessageById(message).queue { msg ->
                auctionMessage = msg

                waiter.countDown()
            }

            waiter.await()
        }
    }
}