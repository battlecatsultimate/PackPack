package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.sharding.ShardManager

object SlotEmojiContainer {
    val loadedEmoji = ArrayList<Emoji>()
    private val registeredServer = ArrayList<Long>()

    fun registerServer(g: Guild) {
        if (g.idLong in registeredServer)
            return

        registeredServer.add(g.idLong)
        loadedEmoji.addAll(g.emojis)
    }

    fun unregisterServer(g: Guild) {
        if (g.idLong !in registeredServer)
            return

        registeredServer.remove(g.idLong)

        val emojis = g.emojis

        loadedEmoji.removeIf { e -> e in emojis }
    }

    fun load(shardManager: ShardManager?) {
        for (guildID in registeredServer) {
            val g = shardManager?.getGuildById(guildID) ?: continue

            loadedEmoji.addAll(g.emojis)
        }
    }

    fun asJson() : JsonArray {
        val arr = JsonArray()

        registeredServer.forEach { id -> arr.add(id) }

        return arr
    }

    fun fromJson(arr: JsonArray) {
        arr.forEach { e -> registeredServer.add(e.asLong) }
    }
}