package mandarin.card.commands

import com.google.gson.JsonPrimitive
import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class TempRestore : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel
        val msg = loader.message

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "Format : `cd.ter [Card ID]`", msg) { a -> a }

            return
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(ch, "Card ID must be numeric!", msg) { a -> a }

            return
        }

        val id = StaticStore.safeParseInt(contents[1])

        val foundCard = CardData.cards.find { c -> c.id == id }

        if (foundCard == null) {
            replyToMessageSafely(ch, "There's no such card with id $id", msg) { a -> a }

            return
        }

        val hasCard = CardData.inventories.values.any { i -> foundCard in i.cards || foundCard in i.favorites }

        if (hasCard) {
            replyToMessageSafely(ch,
                "There are users who already own this card $id. This means that users already got said cards after the rework. You can merge current save and old save. Do you want to merge the save? This cannot be undone",
                msg,
                { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }
            ) { message ->
                StaticStore.putHolder(m.id, ConfirmButtonHolder(msg, m.id, ch.id, message, CommonStatic.Lang.Locale.EN) {
                    restoreCards(foundCard)

                    replyToMessageSafely(ch, "Successfully merged with old card save for card ID $id", msg) { a -> a }
                })
            }
        } else {
            replyToMessageSafely(ch,
                "Are you sure you want to restore card $id status into current card save? This cannot be undone",
                msg,
                { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }
            ) { message ->
                StaticStore.putHolder(m.id, ConfirmButtonHolder(msg, m.id, ch.id, message, CommonStatic.Lang.Locale.EN) {
                    restoreCards(foundCard)

                    replyToMessageSafely(ch, "Successfully restored with old card save for card ID $id", msg) { a -> a }
                })
            }
        }
    }

    private fun restoreCards(card: Card) {
        val obj = StaticStore.getJsonFile("oldCardSave")

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
                val foundCard = CardData.cards.find { c -> c.id == card.id } ?: return

                actualInventory.cards[foundCard] = (actualInventory.cards[foundCard] ?: 0) + (value.cards[foundCard] ?: 0)
                actualInventory.favorites[foundCard] = (actualInventory.favorites[foundCard] ?: 0) + (value.favorites[foundCard] ?: 0)
            }
        }
    }
}