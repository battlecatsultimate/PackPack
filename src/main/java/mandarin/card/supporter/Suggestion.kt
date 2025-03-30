package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import net.dv8tion.jda.api.entities.Member

class Suggestion {
    companion object {
        fun fromJson(obj: JsonObject) : Suggestion {
            val suggestion = Suggestion()

            if (obj.has("cards")) {
                obj.getAsJsonArray("cards").forEach {
                    if (it is JsonObject && it.has("key") && it.has("val")) {
                        val cardID = it.get("key").asInt
                        val foundCard = CardData.cards.find { c -> c.id == cardID } ?: return@forEach

                        val amount = it.get("val").asInt

                        suggestion.cards[foundCard] = amount
                    } else {
                        val foundCard = CardData.cards.find { c -> c.id == it.asInt } ?: return@forEach

                        suggestion.cards[foundCard] = (suggestion.cards[foundCard] ?: 0) + 1
                    }
                }
            }

            if (obj.has("catFood")) {
                suggestion.catFood = obj.get("catFood").asInt
            }

            if (obj.has("touched")) {
                suggestion.touched = obj.get("touched").asBoolean
            }

            return suggestion
        }
    }

    val cards = HashMap<Card, Int>()
    var catFood = 0
    var touched = false

    fun suggestionInfo(member: Member) : String {
        val builder = StringBuilder("Suggestion from ${member.asMention}\n\n### Cards\n\n")

        if (cards.isEmpty()) {
            builder.append("- No Cards\n\n")
        } else {
            cards.forEach { (card, amount) ->
                builder.append("- ")
                    .append(card.cardInfo())

                if (amount > 1) {
                    builder.append(" x")
                        .append(amount)
                }

                builder.append("\n")
            }

            builder.append("\n")
        }

        builder.append("### Cat foods ${EmojiStore.ABILITY["CF"]?.formatted}\n\n")

        if (catFood == 0) {
            builder.append("- No Cat Foods")
        } else {
            val tax = (catFood * CardData.TAX).toInt()
            val actualCf = catFood - tax

            if (tax == 0) {
                builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf")
            } else {
                builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf [Considering 10% tax, ${EmojiStore.ABILITY["CF"]?.formatted} $tax]")
            }
        }

        return builder.toString()
    }

    fun suggestionInfoCompacted() : String {
        val builder = StringBuilder("**Cards**\n\n")

        if (cards.isEmpty()) {
            builder.append("- No Cards\n\n")
        } else {
            cards.forEach { (card, amount) ->
                builder.append("- ")
                    .append(card.cardInfo())

                if (amount > 1) {
                    builder.append(" x")
                        .append(amount)
                }

                builder.append("\n")
            }

            builder.append("\n")

            builder.append("\n")
        }

        builder.append("**Cat foods ${EmojiStore.ABILITY["CF"]?.formatted}**\n\n")

        if (catFood == 0) {
            builder.append("- No Cat Foods")
        } else {
            val tax = (catFood * CardData.TAX).toInt()
            val actualCf = catFood - tax

            if (tax == 0) {
                builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf")
            } else {
                builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf [Considering 10% tax, ${EmojiStore.ABILITY["CF"]?.formatted} $tax]")
            }
        }

        return builder.toString()
    }

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("catFood", catFood)

        val arr = JsonArray()

        cards.forEach { (card, amount) ->
            val o = JsonObject()

            o.addProperty("key", card.id)
            o.addProperty("val", amount)

            arr.add(o)
        }

        obj.add("cards", arr)
        obj.addProperty("touched", touched)

        return obj
    }

    fun copy() : Suggestion {
        val suggestion = Suggestion()

        cards.forEach { (card, amount) ->
            suggestion.cards[card] = amount
        }

        suggestion.catFood = catFood
        suggestion.touched = touched

        return suggestion
    }

    fun paste(suggestion: Suggestion) {
        cards.clear()

        suggestion.cards.forEach { (card, amount) ->
            cards[card] = amount
        }

        catFood = suggestion.catFood

        touched = suggestion.touched
    }

    fun clear() {
        cards.clear()
        catFood = 0
    }
}