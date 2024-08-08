import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files

fun main() {
    val fixedFolder = File("./data/lang/fixed/")

    if (!fixedFolder.exists() && !fixedFolder.mkdirs())
        return

    val enJson = File("./data/lang/en.json")

    val enReader = FileReader(enJson)
    val enObj = JsonParser.parseReader(enReader)

    enReader.close()

    if (enObj !is JsonObject)
        return

    localeCode.forEach { code ->
        val f = File("./data/lang/$code.json")

        val reader = FileReader(f)
        val obj = JsonParser.parseReader(reader)

        reader.close()

        if (obj !is JsonObject)
            return

        injectMissingData(enObj, obj)

        val mapper = ObjectMapper()
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true)

        val json = sortNode(obj).toString()
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

private fun injectMissingData(src: JsonObject, target: JsonObject) {
    src.keySet().forEach { key ->
        if (!target.has(key)) {
            target.add(key, src.get(key))
        } else {
            val subSource = src.get(key)
            val subTarget = target.get(key)

            if (subSource is JsonObject && subTarget is JsonObject) {
                injectMissingData(subSource, subTarget)
            }
        }
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