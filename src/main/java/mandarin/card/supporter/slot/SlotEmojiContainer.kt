package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.slot.SlotMachineCardRewardHolder
import mandarin.card.supporter.holder.slot.SlotMachineCurrencyRewardHolder
import mandarin.card.supporter.holder.slot.SlotMachinePlaceHolderRewardHolder
import mandarin.packpack.supporter.StaticStore
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.CountDownLatch

object SlotEmojiContainer {
    val loadedEmoji = ArrayList<CustomEmoji>()
    val registeredServer = ArrayList<Long>()

    fun registerServer(g: Guild) {
        if (g.idLong in registeredServer)
            return

        g.retrieveEmojis().queue { emojis ->
            loadedEmoji.addAll(emojis)

            updateEmojiStatus()
        }

        registeredServer.add(g.idLong)
    }

    fun unregisterServer(g: Guild) {
        if (g.idLong !in registeredServer)
            return

        registeredServer.remove(g.idLong)

        g.retrieveEmojis().queue { emojis ->
            CardData.slotMachines.forEach { s ->
                var invalidEmojiFound = false

                s.content.forEach { c ->
                    if (emojis.any { e -> e.name == c.emoji?.name && e.id == c.emoji?.id }) {
                        c.changeEmoji(null)

                        invalidEmojiFound = true
                    }
                }

                if (invalidEmojiFound) {
                    s.activate = false
                }
            }

            loadedEmoji.removeIf { e -> emojis.any { emoji -> emoji.name == e.name && emoji.id == e.id } }

            updateEmojiStatus()
        }
    }

    fun updateEmojiAdd(emoji: CustomEmoji) {
        loadedEmoji.add(emoji)

        updateEmojiStatus()
    }

    fun updateEmojiRemoved(emoji: CustomEmoji) {
        loadedEmoji.removeIf { e -> emoji.name == e.name && emoji.idLong == e.idLong }

        CardData.slotMachines.forEach { s ->
            var needDeactivate = false

            s.content.forEach { c ->
                if (c.emoji?.name == emoji.name && c.emoji?.id == emoji.id) {
                    c.changeEmoji(null)

                    needDeactivate = true
                }
            }

            if (needDeactivate) {
                s.activate = false
            }
        }

        updateEmojiStatus()
    }

    fun load(shardManager: ShardManager?) {
        for (guildID in registeredServer) {
            val g = shardManager?.getGuildById(guildID) ?: continue

            val countDown = CountDownLatch(1)

            g.retrieveEmojis().queue({ emojis ->
                loadedEmoji.addAll(emojis)

                countDown.countDown()
            }) { e ->
                StaticStore.logger.uploadErrorLog(e, "E/SlotEmojiContainer::load - Failed to load emoji from guilds")

                countDown.countDown()
            }

            countDown.await()
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

    private fun updateEmojiStatus() {
        StaticStore.holders.forEach { (_, hub) ->
            val componentHolder = hub.componentHolder ?: return@forEach

            when(componentHolder) {
                is SlotMachineCardRewardHolder -> componentHolder.updateEmojiStatus()
                is SlotMachineCurrencyRewardHolder -> componentHolder.updateEmojiStatus()
                is SlotMachinePlaceHolderRewardHolder -> componentHolder.updateEmojiStatus()
            }
        }
    }
}