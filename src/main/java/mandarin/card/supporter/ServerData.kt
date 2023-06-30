package mandarin.card.supporter

import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

object ServerData {
    private lateinit var JSON: JsonObject

    fun initialize() {
        JSON = StaticStore.getJsonFile("serverData")
    }

    fun get(key: String) : String {
        if (!this::JSON.isInitialized) {
            initialize()
        }

        return JSON.get(key).asString
    }

    fun getArray(key: String) : Array<String> {
        if (!JSON.has(key) || !JSON.get(key).isJsonArray)
            return arrayOf()

        val arr = JSON.getAsJsonArray(key)

        return Array(arr.size()) {
            arr[it].asString
        }
    }
}