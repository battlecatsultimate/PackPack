import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import mandarin.packpack.supporter.StaticStore
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files

val localeCode = arrayOf("en", "jp", "kr", "ru", "zh")

fun main() {
    val fixedFolder = File("./data/lang/fixed/")

    if (!fixedFolder.exists() && !fixedFolder.mkdirs())
        return

    localeCode.forEach { code ->
        val f = File("./data/lang/$code.json")

        val reader = FileReader(f)
        val obj = JsonParser.parseReader(reader)

        reader.close()

        if (obj !is JsonObject)
            return

        val newObj = JsonObject()

        obj.keySet().forEach { key ->
            val pathData = key.split(".")
            var o: JsonObject? = null

            pathData.forEachIndexed { index, path ->
                if (index == pathData.lastIndex) {
                    if (o == null) {
                        newObj.addProperty(path, obj.get(key).asString)
                    } else {
                        o.addProperty(path, obj.get(key).asString)
                    }
                } else {
                    o = if (o == null) {
                        if (newObj.has(path)) {
                            newObj.getAsJsonObject(path)
                        } else {
                            val temp = JsonObject()

                            newObj.add(path, temp)

                            temp
                        }
                    } else {
                        if (o.has(path)) {
                            val check = o.get(path)

                            if (check is JsonObject) {
                                check.asJsonObject
                            } else {
                                throw IllegalStateException("${pathData.subList(0, index + 1).joinToString(".")} is already registered as string for $key")
                            }
                        } else {
                            val temp = JsonObject()

                            o.add(path, temp)

                            temp
                        }
                    }
                }
            }
        }

        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

        val json = sortNode(newObj).toString()
        val tree = mapper.readTree(json)

        val targetFile = File(fixedFolder, "$code.json")

        if (!targetFile.exists()) {
            Files.createFile(targetFile.toPath())
        }

        val writer = FileWriter(targetFile)

        writer.write(mapper.writeValueAsString(tree))
        writer.close()
    }
}

private fun sortNode(obj: JsonObject) : JsonObject {
    val elements = ArrayList<Pair<String, JsonElement>>()
    val comparator = StringNumberComparator()

    obj.keySet().forEach { key ->
        val e = obj.get(key)

        if (e is JsonObject) {
            elements.add(Pair(key, sortNode(e)))
        } else {
            elements.add(Pair(key, e))
        }
    }

    elements.sortWith { p1, p2 ->
        if (p1 == null && p2 == null)
            return@sortWith 0

        p1 ?: return@sortWith 1
        p2 ?: return@sortWith -1

        val e1 = p1.second
        val e2 = p2.second

        if (e1 is JsonObject && e2 is JsonObject) {
            return@sortWith comparator.compare(p1.first, p2.first)
        } else {
            if (e1 is JsonObject) {
                return@sortWith 1
            }

            if (e2 is JsonObject) {
                return@sortWith -1
            }

            return@sortWith comparator.compare(p1.first, p2.first)
        }
    }

    val fixed = JsonObject()

    elements.forEach { pair ->
        fixed.add(pair.first, pair.second)
    }

    return fixed
}

class StringNumberComparator : Comparator<String> {
    override fun compare(o1: String?, o2: String?): Int {
        if (o1 == null && o2 == null)
            return 0

        o1 ?: return 1
        o2 ?: return -1

        if (StaticStore.isNumeric(o1) && StaticStore.isNumeric(o2)) {
            return StaticStore.safeParseLong(o1).compareTo(StaticStore.safeParseLong(o2))
        }

        val eliminated1 = eliminateMatchingStarts(o2, o1)
        val eliminated2 = eliminateMatchingStarts(o1, o2)

        if (StaticStore.isNumeric(eliminated1) && StaticStore.isNumeric(eliminated2)) {
            return StaticStore.safeParseLong(eliminated1).compareTo(StaticStore.safeParseLong(eliminated2))
        }

        return eliminated1.compareTo(eliminated2)
    }

    private fun eliminateMatchingStarts(src: String, target: String) : String {
        var matching = 0

        src.forEachIndexed { index, letter ->
            if (index >= target.length)
                return if (matching == target.length)
                    ""
                else
                    target.substring(matching, target.length)

            val l = target[index]

            if (letter == l)
                matching++
            else
                return target.substring(matching, target.length)
        }

        return if (matching == target.length)
            ""
        else
            target.substring(matching, target.length)
    }
}