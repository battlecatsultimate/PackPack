package mandarin.card.supporter.transaction

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mandarin.card.CardBot
import mandarin.packpack.supporter.EmojiStore
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import org.apache.http.client.HttpResponseException
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHeader
import org.apache.http.protocol.HTTP
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI
import java.time.Clock
import java.time.Instant
import kotlin.math.min

object TatsuHandler {
    enum class Action {
        ADD,
        REMOVE
    }

    var API = ""

    // -1 means it's not been used yet
    var leftRequest = -1
    var nextRefreshTime = -1L

    private const val domain = "https://api.tatsu.gg"
    private const val version = "v1"
    private const val guilds = "guilds"
    private const val members = "members"

    private const val points = "points"

    /**
     * Transfer server points to others. Said amount is transferred [member1] -> [member2]
     *
     * @param guild ID of guild
     * @param member1 A member who will transfer points to other member
     * @param member2 A member who will receive points from other member
     * @param amount Amount of points that will be transferred
     *
     * @return Whether method successfully finished the job or not
     */
    @Synchronized
    fun transferPoints(channel: MessageChannel?, guild: Long, member1: Long, member2: Long, amount: Int) {
        if (API.isBlank()) {
            throw IllegalStateException("API key isn't prepared yet")
        }

        var leftAmount = amount

        var cost = 2

        while (leftAmount > 100000) {
            leftAmount -= 100000
            cost += 2
        }

        if (!canInteract(cost, false)) {
            TransactionGroup.queue(TransactionQueue(cost) {
                leftAmount = amount

                var removal = true
                var add = true

                while(leftAmount > 0) {
                    val possibleAmount = min(100000, leftAmount)

                    removal = removal && modifyPoints(guild, member1, possibleAmount, Action.REMOVE, true)
                    add = add && modifyPoints(guild, member2, possibleAmount, Action.ADD, true)

                    leftAmount -= possibleAmount
                }

                TransactionLogger.logPointsTransfer(member1, member2, removal, add)

                if (removal && add) {
                    channel?.sendMessage("Successfully transferred ${EmojiStore.ABILITY["CF"]?.formatted} $amount from <@${member1}> to <@${member2}>")
                        ?.setAllowedMentions(ArrayList())
                        ?.queue()
                } else {
                    channel?.sendMessage("There was problem while performing cat food transferring. Please contact moderators")?.queue()
                }
            })
        } else {
            leftAmount = amount

            var removal = true
            var add = true

            while(leftAmount > 0) {
                val possibleAmount = min(100000, leftAmount)

                removal = removal && modifyPoints(guild, member1, possibleAmount, Action.REMOVE, false)
                add = add && modifyPoints(guild, member2, possibleAmount, Action.ADD, false)

                leftAmount -= possibleAmount
            }

            TransactionLogger.logPointsTransfer(member1, member2, removal, add)

            if (removal && add) {
                channel?.sendMessage("Successfully transferred ${EmojiStore.ABILITY["CF"]} $amount from <@${member1}> to <@${member2}>")
                    ?.setAllowedMentions(ArrayList())
                    ?.queue()
            } else {
                channel?.sendMessage("There was problem while performing cat food transferring. Please contact moderators")?.queue()
            }
        }
    }

    fun getPoints(guild: Long, member: Long, beingQueued: Boolean) : Int {
        if (API.isBlank()) {
            throw IllegalStateException("API key isn't prepared yet")
        }

        if (!canInteract(1, beingQueued)) {
            throw IllegalStateException("Interaction can't be done at this time due to limitation")
        }

        val link = "$domain/$version/$guilds/$guild/$members/$member/$points"

        val get = HttpGet()

        get.uri = URI(link)

        get.setHeader("Authorization", API)

        val client = HttpClientBuilder.create().build() as CloseableHttpClient

        val response = client.execute(get) as CloseableHttpResponse
        val status = response.statusLine

        if (status.statusCode / 100 != 2) {
            throw HttpResponseException(status.statusCode, status.reasonPhrase)
        }

        for(header in response.allHeaders) {
            when(header.name) {
                "x-ratelimit-remaining" -> leftRequest = header.value.toInt()
                "x-ratelimit-reset" -> nextRefreshTime = header.value.toLong()
            }
        }

        CardBot.saveCardData()

        val responseBuilder = StringBuilder()

        val responseReader = BufferedReader(InputStreamReader(response.entity.content))
        var line = ""

        while(responseReader.readLine()?.also { line = it } != null) {
            responseBuilder.append(line).append("\n")
        }

        responseReader.close()
        response.close()
        client.close()

        val element = JsonParser.parseString(responseBuilder.toString())

        if (!element.isJsonObject) {
            throw IllegalStateException("Invalid JSON response from the server\n\nJson : $responseBuilder")
        }

        val obj = element.asJsonObject

        if (!obj.has(points)) {
            throw IllegalStateException("Invalid JSON response from the server\nReason : Can't find point from respond data\n\nJson : $responseBuilder")
        }

        return obj.get("points").asInt
    }

    fun modifyPoints(guild: Long, member: Long, amount: Int, action: Action, beingQueued: Boolean) : Boolean {
        if (API.isBlank()) {
            throw IllegalStateException("API key isn't prepared yet")
        }

        if (!canInteract(1, beingQueued)) {
            throw IllegalStateException("Interaction can't be done at this time due to limitation")
        }

        if (amount !in 1..100000) {
            throw IllegalStateException("Amount of points must be in range from 1 to 100000")
        }

        val link = "$domain/$version/$guilds/$guild/$members/$member/$points"

        val patch = HttpPatch()

        patch.uri = URI(link)

        patch.setHeader("Authorization", API)

        val request = JsonObject()

        request.addProperty("action", action.ordinal)
        request.addProperty("amount", amount)

        val entity = StringEntity(request.toString())
        entity.contentType = BasicHeader(HTTP.CONTENT_TYPE, "application/json")

        patch.entity = entity

        val client = HttpClientBuilder.create().build() as CloseableHttpClient

        val response = client.execute(patch) as CloseableHttpResponse
        val status = response.statusLine

        if (status.statusCode / 100 != 2) {
            if (response.entity.contentLength < 0) {
                val responseBuilder = StringBuilder()

                val responseReader = BufferedReader(InputStreamReader(response.entity.content))
                var line = ""

                while(responseReader.readLine()?.also { line = it } != null) {
                    responseBuilder.append(line).append("\n")
                }

                responseReader.close()
                response.close()
                client.close()

                throw HttpResponseException(status.statusCode, status.reasonPhrase + "\n$responseBuilder")
            } else {
                response.close()
                client.close()

                throw HttpResponseException(status.statusCode, status.reasonPhrase)
            }
        }

        for(header in response.allHeaders) {
            when(header.name) {
                "x-ratelimit-remaining" -> leftRequest = header.value.toInt()
                "x-ratelimit-reset" -> nextRefreshTime = header.value.toLong()
            }
        }

        CardBot.saveCardData()

        val responseBuilder = StringBuilder()

        val responseReader = BufferedReader(InputStreamReader(response.entity.content))
        var line = ""

        while(responseReader.readLine()?.also { line = it } != null) {
            responseBuilder.append(line).append("\n")
        }

        responseReader.close()
        response.close()
        client.close()

        val element = JsonParser.parseString(responseBuilder.toString())

        if (!element.isJsonObject) {
            throw IllegalStateException("Invalid JSON response from the server\n\nJson : $responseBuilder")
        }

        val obj = element.asJsonObject

        if (!obj.has(points)) {
            throw IllegalStateException("Invalid JSON response from the server\nReason : Can't find point from respond data\n\nJson : $responseBuilder")
        }

        return true
    }

    fun canInteract(cost: Int, beingQueued: Boolean) : Boolean {
        if (leftRequest == -1)
            return true

        return (beingQueued || TransactionGroup.groupQueue.isEmpty()) && (leftRequest >= cost || Instant.now(Clock.systemUTC()).epochSecond >= nextRefreshTime)
    }
}