package mandarin.card.supporter.slot

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.packpack.supporter.StaticStore

class SlotMachine {
    companion object {
        fun fromJson(obj: JsonObject) : SlotMachine {
            if (!StaticStore.hasAllTag(obj, "cooldown", "slotSize", "price", "content")) {
                throw IllegalStateException("E/SlotMachine::fromJson - Invalid json data found")
            }

            val slotMachine = SlotMachine()

            slotMachine.cooldown = obj.get("cooldown").asLong
            slotMachine.slotSize = obj.get("slotSize").asLong
            slotMachine.price = SlotPrice.fromJson(obj.getAsJsonObject("price"))

            val arr = obj.getAsJsonArray("content")

            arr.forEach { e ->
                slotMachine.content.add(SlotContent.fromJson(e.asJsonObject))
            }

            return slotMachine
        }
    }

    var cooldown = 0L
    var slotSize = 3L
    var price = SlotPrice()
        private set
    val content = ArrayList<SlotContent>()

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("cooldown", cooldown)
        obj.addProperty("slotSize", slotSize)
        obj.add("price", price.asJson())

        val arr = JsonArray()

        content.forEach { content ->
            arr.add(content.asJson())
        }

        obj.add("content", arr)

        return obj
    }
}