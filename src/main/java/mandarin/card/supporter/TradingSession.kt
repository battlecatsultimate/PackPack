package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.CardBot
import mandarin.card.supporter.log.TransactionLogger
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

class TradingSession(val postID: Long, val member: Array<Long>) {
    companion object {
        fun fromJson(obj: JsonObject) : TradingSession {
            if (!obj.has("postID") || !obj.has("members"))
                return TradingSession(-1, Array(0) { 0 })

            val postID = obj.get("postID").asLong

            val memberArray = obj.getAsJsonArray("members")

            val member = Array(2) {
                memberArray[it].asLong
            }

            val session = TradingSession(postID, member)

            if (obj.has("agreements")) {
                obj.getAsJsonArray("agreements").forEachIndexed { index, element ->
                    session.agreed[index] = element.asBoolean
                }
            }

            if (obj.has("suggestions")) {
                obj.getAsJsonArray("suggestions").forEachIndexed { index, element ->
                    session.suggestion[index] = Suggestion.fromJson(element.asJsonObject)
                }
            }

            if (obj.has("confirmNotified")) {
                session.confirmNotified = obj.get("confirmNotified").asBoolean
            }

            if (obj.has("approved")) {
                session.approved = obj.get("approved").asBoolean
            }

            return session
        }

        fun accumulateSuggestedCatFood(member: Long) : Int {
            return CardData.sessions.filter { s -> s.member.contains(member) }.map { s -> s.suggestion[s.member.indexOf(member)] }.sumOf { s -> s.catFood }
        }
    }

    val agreed = booleanArrayOf(false, false)
    val suggestion = Array(2) {
        Suggestion()
    }
    var approved = false

    var confirmNotified = false

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("postID", postID)

        val members = JsonArray()

        members.add(member[0])
        members.add(member[1])

        obj.add("members", members)

        val agreements = JsonArray()

        agreements.add(agreed[0])
        agreements.add(agreed[1])

        obj.add("agreements", agreements)

        val suggestions = JsonArray()

        suggestions.add(suggestion[0].toJson())
        suggestions.add(suggestion[1].toJson())

        obj.add("suggestions", suggestions)

        obj.addProperty("confirmNotified", confirmNotified)

        obj.addProperty("approved", approved)

        return obj
    }

    fun validate(ch: MessageChannel) : Boolean {
        val thisInventory = Inventory.getInventory(member[0])
        val thatInventory = Inventory.getInventory(member[1])

        suggestion[0].cards.forEach { (card, amount) ->
            if (!thisInventory.cards.containsKey(card)) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[0]}>'s inventory doesn't have card\nCard : ${card.cardInfo()}")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }

            val currentAmount = thisInventory.cards[card] ?: 0

            if (currentAmount - amount < 0) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[0]}>'s inventory doesn't have enough card to trade\nCard : ${card.cardInfo()}\nRequired amount : $amount")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }
        }

        suggestion[1].cards.forEach { (card, amount) ->
            if (!thatInventory.cards.containsKey(card)) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[1]}>'s inventory doesn't have card\nCard : ${card.cardInfo()}")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }

            val currentAmount = thatInventory.cards[card] ?: 1

            if (currentAmount - amount < 0) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[1]}>'s inventory doesn't have enough card to trade\nCard : ${card.cardInfo()}\nRequired amount : $amount")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }
        }

        return true
    }

    fun trade() {
        val thisInventory = Inventory.getInventory(member[0])
        val thatInventory = Inventory.getInventory(member[1])

        suggestion[1].cards.forEach { (card, amount) ->
            thisInventory.cards[card] = (thisInventory.cards[card] ?: 0) + amount
            thatInventory.cards[card] = (thatInventory.cards[card] ?: 0) - amount
        }

        suggestion[0].cards.forEach { (card, amount) ->
            thatInventory.cards[card] = (thatInventory.cards[card] ?: 0) + amount
            thisInventory.cards[card] = (thisInventory.cards[card] ?: 0) - amount
        }

        //Transfer Cat food
        if (suggestion[0].catFood != 0) {
            thisInventory.catFoods -= suggestion[0].catFood
            thatInventory.catFoods += suggestion[0].catFood
        }

        if (suggestion[1].catFood != 0) {
            thisInventory.catFoods += suggestion[1].catFood
            thatInventory.catFoods -= suggestion[1].catFood
        }

        //Validate inventory
        thisInventory.cards.values.removeAll { it <= 0 }
        thatInventory.cards.values.removeAll { it <= 0 }
    }

    fun close(ch: MessageChannel) {
        if (ch is ThreadChannel) {
            ch.manager.setLocked(true).setArchived(true).queue()
        }

        CardData.sessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logTrade(this, TransactionLogger.TradeStatus.TRADED)
    }

    fun expire() {
        CardData.sessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logTrade(this, TransactionLogger.TradeStatus.EXPIRED)
    }

    fun expire(ch: MessageChannel) {
        ch.sendMessage("This session is closed due to expiration. Please don't leave session opened for long time (5 days)").queue()

        if (ch is ThreadChannel) {
            ch.manager.setLocked(true).setArchived(true).queue()
        }

        CardData.sessions.remove(this)

        CardBot.saveCardData()

        TransactionLogger.logTrade(this, TransactionLogger.TradeStatus.EXPIRED)
    }

    fun needApproval(g: Guild, whenNeed: Runnable, otherwise: Runnable) {
        g.retrieveMembers(member.map { id -> UserSnowflake.fromId(id) }).onSuccess { members ->
            if (suggestion.any { s -> s.catFood >= 200000 || s.cards.any { (c, _) -> c.tier == CardData.Tier.SPECIAL || c.tier == CardData.Tier.LEGEND } } && !members.any { m -> CardData.hasAllPermission(m) } && !approved) {
                whenNeed.run()
            } else {
                otherwise.run()
            }
        }
    }
}