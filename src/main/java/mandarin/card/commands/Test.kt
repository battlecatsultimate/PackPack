package mandarin.card.commands

import com.google.gson.JsonPrimitive
import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.UserSnowflake

class Test : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL)
            return

        val obj = StaticStore.getJsonFile("cardSave")

        if (obj.has("inventory")) {
            val arr = obj.getAsJsonArray("inventory")

            for (i in 0 until arr.size()) {
                val pair = arr[i].asJsonObject

                if (!pair.has("key") || !pair.has("val"))
                    continue

                val keyData = pair["key"]

                if (keyData !is JsonPrimitive)
                    continue

                val key = if (keyData.isString) {
                    keyData.asString.toLong()
                } else {
                    keyData.asLong
                }

                val value = Inventory.readInventory(key, pair["val"].asJsonObject)
                
                val actualInventory = Inventory.getInventory(key)
                val axel = CardData.cards.find { c -> c.id == 774 } ?: return

                actualInventory.cards[axel] = value.cards[axel] ?: 0
            }
        }
    }
}