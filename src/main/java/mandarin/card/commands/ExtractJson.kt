package mandarin.card.commands

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import java.io.FileWriter

class ExtractJson : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid"))
            return

        val userID = getUserID(loader.content.split(" "))

        if (userID == -1L) {
            replyToMessageSafely(loader.channel, "Failed to get user ID from the command. Please provide valid user ID", loader.message) { a -> a }

            return
        }

        CardBot.saveCardData()

        val json = if (CardBot.test) {
            StaticStore.getJsonFile("testCardSave")
        } else {
            StaticStore.getJsonFile("cardSave")
        }

        val result = JsonObject()

        extractJsonObject(json, result, userID, true)

        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

        val nodes = mapper.readTree(result.toString())

        val folder = File("./temp")

        if (!folder.exists() && !folder.mkdirs()) {
            StaticStore.logger.uploadLog("W/ExtractJson::doSomething - Failed to create folder ${folder.absolutePath}")
            replyToMessageSafely(loader.channel, "Failed to create json file", loader.message) { a -> a }

            return
        }

        val file = StaticStore.generateTempFile(folder, "extracted$userID", "json", false)

        val writer = FileWriter(file)

        writer.append(mapper.writeValueAsString(nodes))

        writer.close()

        replyToMessageSafely(loader.channel, "Extracted json file", loader.message, { a ->
            a.setFiles(FileUpload.fromData(file, "extracted$userID.json"))
        }) { _ ->
            StaticStore.deleteFile(file, true)
        }
    }

    private fun getUserID(contents: List<String>) : Long {
        return StaticStore.safeParseLong(contents.map { s -> s.replace("<@", "").replace(">", "") }.find { s -> StaticStore.isNumeric(s) } ?: "-1")
    }

    private fun extractJsonObject(from: JsonObject, to: JsonObject, id: Long, root: Boolean) {
        var userIDFound = false

        run breaker@ {
            from.keySet().forEach { key ->
                val e = from.get(key)

                if (e is JsonPrimitive) {
                    if (e.isNumber && e.asLong == id) {
                        if (root) {
                            to.add(key, e)
                        } else {
                            userIDFound = true
                            return@breaker
                        }
                    } else if (e.isString && e.asString == id.toString()) {
                        if (root) {
                            to.add(key, e)
                        } else {
                            userIDFound = true
                            return@breaker
                        }
                    }
                } else if (e is JsonObject) {
                    val subObject = JsonObject()

                    extractJsonObject(e, subObject, id, false)

                    if (!subObject.isEmpty)
                        if (root) {
                            to.add(key, subObject)
                        } else {
                            userIDFound = true
                            return@breaker
                        }
                } else if (e is JsonArray) {
                    val subArray = JsonArray()

                    extractJsonArray(e, subArray, id)

                    if (!subArray.isEmpty)
                        if (root) {
                            to.add(key, subArray)
                        } else {
                            userIDFound = true
                            return@breaker
                        }
                }
            }
        }

        if (userIDFound) {
            HashSet(to.keySet()).forEach { key ->
                to.remove(key)
            }

            from.keySet().forEach { key ->
                val e = from.get(key)

                to.add(key, e)
            }
        }
    }

    private fun extractJsonArray(from: JsonArray, to: JsonArray, id: Long) {
        from.forEach { e ->
            if (e is JsonPrimitive) {
                if (e.isNumber && e.asLong == id) {
                    to.add(e)
                } else if (e.isString && e.asString == id.toString()) {
                    to.add(e)
                }
            } else if (e is JsonObject) {
                val subObject = JsonObject()

                extractJsonObject(e, subObject, id, false)

                if (!subObject.isEmpty)
                    to.add(subObject)
            } else if (e is JsonArray) {
                val subArray = JsonArray()

                extractJsonArray(e, subArray, id)

                if (!subArray.isEmpty)
                    to.add(subArray)
            }
        }
    }
}