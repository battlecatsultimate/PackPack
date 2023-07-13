package mandarin.card.supporter

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import mandarin.packpack.supporter.EmojiStore
import net.dv8tion.jda.api.entities.Member

class Suggestion {
    companion object {
        fun fromJson(obj: JsonObject) : Suggestion {
            val suggestion = Suggestion()

            if (obj.has("cards")) {
                obj.getAsJsonArray("cards").forEach {
                    val foundCard = CardData.cards.find { c -> c.unitID == it.asInt }

                    if (foundCard != null) {
                        suggestion.cards.add(foundCard)
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

    val cards = ArrayList<Card>()
    var catFood = 0
    var touched = false

    fun suggestionInfo(member: Member) : String {
        val builder = StringBuilder("Suggestion from ${member.asMention}\n\n### Cards\n\n")

        if (cards.isEmpty()) {
            builder.append("- No Cards\n\n")
        } else {
            for (card in cards) {
                builder.append("- ")
                    .append(card.cardInfo())
                    .append("\n")
            }

            builder.append("\n")
        }

        builder.append("### Cat foods ${EmojiStore.ABILITY["CF"]?.formatted}\n\n")

        if (catFood == 0) {
            builder.append("- No Cat Foods")
        } else {
            val tax = (catFood * CardData.TAX).toInt()
            val actualCf = catFood - tax

            builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf [Considering 10% tax, ${EmojiStore.ABILITY["CF"]?.formatted} $tax]")
        }

        return builder.toString()
    }

    fun suggestionInfoCompacted() : String {
        val builder = StringBuilder("**Cards**\n\n")

        if (cards.isEmpty()) {
            builder.append("- No Cards\n\n")
        } else {
            for (card in cards) {
                builder.append("- ")
                    .append(card.cardInfo())
                    .append("\n")
            }

            builder.append("\n")
        }

        builder.append("**Cat foods ${EmojiStore.ABILITY["CF"]?.formatted}**\n\n")

        if (catFood == 0) {
            builder.append("- No Cat Foods")
        } else {
            val tax = (catFood * CardData.TAX).toInt()
            val actualCf = catFood - tax

            builder.append("- ${EmojiStore.ABILITY["CF"]?.formatted} $actualCf [Considering 10% tax, ${EmojiStore.ABILITY["CF"]?.formatted} $tax]")
        }

        return builder.toString()
    }

    fun toJson() : JsonObject {
        val obj = JsonObject()

        obj.addProperty("catFood", catFood)

        val arr = JsonArray()

        for (card in cards) {
            arr.add(card.unitID)
        }

        obj.add("cards", arr)
        obj.addProperty("touched", touched)

        return obj
    }

    fun copy() : Suggestion {
        val suggestion = Suggestion()

        suggestion.cards.addAll(cards)
        suggestion.catFood = catFood
        suggestion.touched = touched

        return suggestion
    }

    fun paste(suggestion: Suggestion) {
        cards.clear()
        cards.addAll(suggestion.cards)

        catFood = suggestion.catFood

        touched = suggestion.touched
    }

    fun clear() {
        cards.clear()
        catFood = 0
    }
}