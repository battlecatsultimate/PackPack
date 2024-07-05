package mandarin.card.supporter.card

import com.google.gson.JsonObject
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.PackCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import java.io.File
import java.io.IOException
import java.nio.file.Files

class Skin {
    companion object {
        fun fromJson(obj: JsonObject) : Skin {
            if (!StaticStore.hasAllTag(obj, "cost", "public", "creator", "card", "skinID", "name")) {
                throw IllegalStateException("E/Skin::fromJson - Invalid json format")
            }

            val cost = PackCost.fromJson(obj.getAsJsonObject("cost"))
            val public = obj.get("public").asBoolean
            val creator = obj.get("creator").asLong

            val cardID = obj.get("card").asInt

            val card = CardData.cards.find { c -> c.unitID == cardID } ?: throw NullPointerException("E/Skin::fromJson - Failed to find card $cardID")

            val skinID = obj.get("skinID").asInt
            val name = obj.get("name").asString

            val folder = File("./data/cards/Skin/")
            val files = folder.listFiles() ?: throw NullPointerException("E/Skin::fromJson - Failed to list skin folder")

            val file = files.find { f -> StaticStore.isNumeric(f.nameWithoutExtension) && f.nameWithoutExtension.toInt() == skinID }
                ?: throw NullPointerException("E/skin::fromJson - Failed to find skin file $skinID")

            val skin = Skin(skinID, file, creator, card, cost)

            skin.public = public
            skin.name = name

            return skin
        }
    }

    val cost: PackCost
    var public = true
    var creator: Long
    val card: Card
    val skinID: Int
    var name: String
    var file: File

    constructor(card: Card, file: File) {
        cost = PackCost(0L, 0L, ArrayList(), ArrayList())
        creator = -1L

        this.card = card

        var maxID = 1

        CardData.skins.filter { skin -> skin.card == card }.forEach { skin ->
            if (maxID <= skin.skinID / 10000) {
                maxID = skin.skinID / 10000 + 1
            }
        }

        skinID = if (card.unitID < 0) {
            -10000 * maxID + card.unitID
        } else {
            10000 * maxID + card.unitID
        }

        val folder = File("./data/cards/Skin/")

        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("E/Skin::init - Failed to create folder : ${folder.absolutePath}")
        }

        val newFile = File(folder, "$skinID.${file.extension}")

        if (newFile.exists() && !newFile.delete()) {
            throw IOException("E/Skin::init - $skinID was existing, but failed to delete")
        }

        Files.move(file.toPath(), newFile.toPath())

        this.file = newFile
        name = "Skin $skinID"
    }

    private constructor(skinID: Int, skinFile: File, creatorID: Long, card: Card, cost: PackCost) {
        this.creator = creatorID
        this.card = card
        this.skinID = skinID
        this.file = skinFile
        this.cost = cost

        name = "Skin $skinID"
    }

    fun displayInfo(showPublic: Boolean) : String {
        val builder = StringBuilder()

        builder.append("**Targeted Card** : ").append(card.simpleCardInfo()).append("\n\n")
            .append("**Skin ID** : ").append(skinID).append("\n")
            .append("**Skin Name** : ").append(name).append("\n")
            .append("**Creator** : ")

        if (creator == -1L) {
            builder.append("<@").append(CardData.bankAccount).append("> [Official Skin]\n\n")
        } else {
            builder.append("<@").append(creator).append(">\n\n")
        }

        if (showPublic) {
            builder.append("**Is Public?** : ")

            if (public) {
                builder.append("Yes")
            } else {
                builder.append("No")
            }

            builder.append("\n")
        }

        builder.append("**Cost** : ")

        if (cost.catFoods <= 0L && cost.platinumShards <= 0L && cost.cardsCosts.isEmpty()) {
            builder.append("Free")
        } else {
            builder.append("\n")

            if (cost.catFoods != 0L) {
                builder.append("- ").append(EmojiStore.ABILITY["CF"]?.formatted).append(" ").append(cost.catFoods).append("\n")
            }

            if (cost.platinumShards != 0L) {
                builder.append("- ").append(EmojiStore.ABILITY["SHARD"]?.formatted).append(" ").append(cost.platinumShards).append("\n")
            }

            if (cost.cardsCosts.isNotEmpty()) {
                cost.cardsCosts.forEach { cost ->
                    builder.append("- ").append(cost.getCostName()).append("\n")
                }
            }
        }

        return builder.toString().trim()
    }

    fun asJson() : JsonObject {
        val obj = JsonObject()

        obj.add("cost", cost.asJson())
        obj.addProperty("public", public)
        obj.addProperty("creator", creator)
        obj.addProperty("card", card.unitID)
        obj.addProperty("skinID", skinID)
        obj.addProperty("name", name)


        return obj
    }
}