package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.transaction.TatsuHandler
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

        return obj
    }

    fun validate(ch: MessageChannel) : Boolean {
        val thisInventory = Inventory.getInventory(member[0].toString())
        val thatInventory = Inventory.getInventory(member[1].toString())

        for (card in suggestion[0].cards) {
            if (!thisInventory.cards.containsKey(card)) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[0]}>'s inventory doesn't have card\nCard : ${card.cardInfo()}")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }

            val amount = thisInventory.cards[card] ?: 1
            val required = suggestion[0].cards.filter { c -> c.id == card.id }.size

            if (amount - required < 0) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[0]}>'s inventory doesn't have enough card to trade\nCard : ${card.cardInfo()}\nRequired amount : $required")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }
        }

        for (card in suggestion[1].cards) {
            if (!thatInventory.cards.containsKey(card)) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[1]}>'s inventory doesn't have card\nCard : ${card.cardInfo()}")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }

            val amount = thatInventory.cards[card] ?: 1
            val required = suggestion[1].cards.filter { c -> c.id == card.id }.size

            if (amount - required < 0) {
                ch.sendMessage("Trading couldn't be done\n\nReason : <@${member[1]}>'s inventory doesn't have enough card to trade\nCard : ${card.cardInfo()}\nRequired amount : $required")
                    .setAllowedMentions(ArrayList())
                    .queue()

                return false
            }
        }

        return true
    }

    fun trade(ch: MessageChannel, guild: Long) {
        val thisInventory = Inventory.getInventory(member[0].toString())
        val thatInventory = Inventory.getInventory(member[1].toString())

        for (card in suggestion[1].cards) {
            thisInventory.cards[card] = (thisInventory.cards[card] ?: 0) + 1
            thatInventory.cards[card] = (thatInventory.cards[card] ?: 1) - 1
        }

        for (card in suggestion[0].cards) {
            thatInventory.cards[card] = (thatInventory.cards[card] ?: 0) + 1
            thisInventory.cards[card] = (thisInventory.cards[card] ?: 1) - 1
        }

        //Transfer Cat food
        if (suggestion[0].catFood != 0) {
            TatsuHandler.transferPoints(ch, guild, member[0], member[1], suggestion[0].catFood)
        }

        if (suggestion[1].catFood != 0) {
            TatsuHandler.transferPoints(ch, guild, member[1], member[0], suggestion[1].catFood)
        }

        //Validate inventory
        thisInventory.cards.values.removeAll { it <= 0 }
        thatInventory.cards.values.removeAll { it <= 0 }


    }
}