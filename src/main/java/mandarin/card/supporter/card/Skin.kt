package mandarin.card.supporter.card

import com.google.gson.JsonObject
import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.PackCost
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