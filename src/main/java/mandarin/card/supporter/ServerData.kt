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
}