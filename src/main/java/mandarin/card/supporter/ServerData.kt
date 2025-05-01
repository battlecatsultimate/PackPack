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

    fun getArray(key: String) : ArrayList<String> {
        if (!JSON.has(key) || !JSON.get(key).isJsonArray)
            return arrayListOf()

        val arr = JSON.getAsJsonArray(key)
        val result = ArrayList<String>()

        result.addAll(arr.map { e -> e.asString })

        return result
    }
}