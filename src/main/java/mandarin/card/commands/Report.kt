package mandarin.card.commands

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.min

class Report : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val time = CardData.getUnixEpochTime()

        val sessionNumber = getSessionNumber(loader.content)

        val sessions = LogSession.gatherPreviousSessions(time, sessionNumber)

        val members = HashSet<Long>()

        sessions.forEach { session ->
            members.addAll(session.activeMembers)
        }

        val consumedCatFoodPack = sessions.sumOf { session -> session.catFoodPack.values.sum() }

        val gainedCatFoodCraft = sessions.sumOf { session -> session.catFoodCraft.values.sum() }

        val totalCraftFailures = sessions.sumOf { session -> session.craftFailures }

        val totalGeneratedCards = sessions.sumOf { session -> session.generatedCards.entries.sumOf { (_, map) -> map.entries.sumOf { (_, amount) -> amount } } }
        val totalRemovedCards = sessions.sumOf { session -> session.removedCards.entries.sumOf { (_, map) -> map.entries.sumOf { (_, amount) -> amount } } }
        val totalCards = sessions.sumOf { session ->
            val cardMap = HashMap<Card, Long>()

            session.generatedCards.forEach { (_, map) -> map.forEach { (card, amount) -> cardMap[card] = (cardMap[card] ?: 0) + amount } }
            session.removedCards.forEach { (_, map) -> map.forEach { (card, amount) -> cardMap[card] = (cardMap[card] ?: 0) - amount } }

            cardMap.entries.sumOf { (_, amount) -> amount }
        }

        val totalCatFoodFlow = sessions.sumOf { session -> session.catFoodTradeSum }

        if (loader.content.contains("-f")) {
            val mergedCatFoodPackTemp = HashMap<Long, Long>()
            val mergedCatFoodCraftTemp = HashMap<Long, Long>()
            val mergedCatFoodTradeTemp = HashMap<Long, Long>()

            val mergedTier2CardsTemp = HashMap<Card, Long>()

            val mergedGeneratedCardsTemp = HashMap<Card, Long>()
            val mergedGeneratedCardsUserTemp = HashMap<Long, HashMap<Card, Long>>()
            val mergedRemovedCardsTemp = HashMap<Card, Long>()
            val mergedRemovedCardsUserTemp = HashMap<Long, HashMap<Card, Long>>()

            sessions.forEach { session ->
                session.catFoodPack.forEach { (id, amount) ->
                    mergedCatFoodPackTemp[id] = (mergedCatFoodPackTemp[id] ?: 0) + amount
                }

                session.catFoodCraft.forEach { (id, amount) ->
                    mergedCatFoodCraftTemp[id] = (mergedCatFoodCraftTemp[id] ?: 0) + amount
                }

                session.catFoodTrade.forEach { (id, amount) ->
                    mergedCatFoodTradeTemp[id] = (mergedCatFoodTradeTemp[id] ?: 0) + amount
                }

                session.generatedCards.forEach { (id, cardMap) ->
                    val cardMap = mergedGeneratedCardsUserTemp[id] ?: run {
                        val map = HashMap<Card, Long>()

                        mergedGeneratedCardsUserTemp[id] = map

                        map
                    }

                    cardMap.forEach { (card, amount) ->
                        mergedGeneratedCardsTemp[card] = (mergedGeneratedCardsTemp[card] ?: 0) + amount
                    }
                }

                session.removedCards.forEach { (_, cardMap) ->
                    cardMap.forEach { (card, amount) ->
                        mergedRemovedCardsTemp[card] = (mergedRemovedCardsTemp[card] ?: 0) + amount
                    }
                }
            }

            val mergedCatFoodPack = mergedCatFoodPackTemp.entries.sortedBy { entry -> entry.value }.reversed()
            val mergedCatFoodCraft = mergedCatFoodCraftTemp.entries.sortedBy { entry -> entry.value }.reversed()
            val mergedCatFoodTrade = mergedCatFoodTradeTemp.entries.sortedBy { entry -> entry.value }.reversed()

            val comparator = CardComparator()

            val mapComparator = Comparator<Map.Entry<Card, Long>> { o1, o2 ->
                if (o1 == null && o2 == null)
                    return@Comparator 0

                if (o1 == null)
                    return@Comparator 1

                if (o2 == null)
                    return@Comparator -1

                return@Comparator if (o1.value != o2.value)
                    -o1.value.compareTo(o2.value)
                else
                    -comparator.compare(o1.key, o2.key)
            }

            val mergedTier2Cards = mergedTier2CardsTemp.entries.sortedWith(mapComparator)

            val mergedGeneratedCards = mergedGeneratedCardsTemp.entries.sortedWith(mapComparator)
            val mergedRemovedCards = mergedRemovedCardsTemp.entries.sortedWith(mapComparator)

            val reporter = StringBuilder(
                "Gathered ${sessions.size} log sessions in total before ${LogSession.globalFormat.format(time)}\n" +
                        "\n" +
                        "========== REPORT ==========\n" +
                        "\n" +
                        "${members.size} members participated BCTC\n" +
                        "\n" +
                        "List of participated user : \n" +
                        "\n"
            )

            val memberIds = members.map { id -> UserSnowflake.fromId(id) }

            var requestSize = memberIds.size / 100

            if (memberIds.size % 100 != 0)
                requestSize++

            val running = BooleanArray(requestSize) { true }

            val memberSet = ArrayList<Member>()

            for (i in 0 until requestSize) {
                loader.guild.retrieveMembers(memberIds.subList(100 * i, min(memberIds.size, 100 * (i + 1)))).onSuccess {
                    memberSet.addAll(it)

                    running[i] = false
                }
            }

            while(true) {
                if (!running.any { it }) {
                    break
                }
            }

            memberSet.sortBy { it.effectiveName }

            val memberList = memberSet.toSet()

            memberList.forEach { member ->
                reporter.append(member.effectiveName).append(" [").append(member.id).append("]\n")
            }

            reporter.append("\n" +
                    "Out of these people :\n" +
                    "\n" +
                    "$consumedCatFoodPack CF have been consumed for generating pack\n" +
                    "\n")

            if (mergedCatFoodPack.isNotEmpty()) {
                reporter.append("Detailed information about consumed cat food for rolling the pack\n" +
                        "\n")

                mergedCatFoodPack.forEach { (id, amount) ->
                    val member = memberList.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(member.effectiveName).append(" [").append(member.id).append("] : ").append(amount).append("\n")
                    } else {
                        reporter.append("UNKNOWN USER [").append(id).append("] : ").append(amount).append("\n")
                    }
                }

                reporter.append("\n")
            }

            reporter.append(
                "$gainedCatFoodCraft CF have been given out for compensation from crafting\n" +
                        "\n"
            )

            if (mergedCatFoodCraft.isNotEmpty()) {
                reporter.append("Detailed information about gained cat food for crafting\n" +
                        "\n"
                )

                mergedCatFoodCraft.forEach { (id, amount) ->
                    val member = memberList.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(member.effectiveName).append(" [").append(member.id).append("] : ").append(amount).append("\n")
                    } else {
                        reporter.append("UNKNOWN USER [").append(id).append("] : ").append(amount).append("\n")
                    }
                }

                reporter.append("\n")
            }

            if (mergedTier2Cards.isNotEmpty()) {
                reporter.append("Detailed information about crafted T2 cards\n" +
                        "\n")

                mergedTier2Cards.forEach { (card, amount) ->
                    reporter.append(card.simpleCardInfo())

                    if (amount > 1)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")
                }

                reporter.append("\n")
            }

            reporter.append("$totalGeneratedCards cards have been generated, and added into economy\n")

            if (mergedGeneratedCards.isNotEmpty()) {
                reporter.append("\n" +
                        "Detailed information about generated cards\n" +
                        "\n"
                )

                mergedGeneratedCards.forEach { (card, amount) ->
                    reporter.append(card.simpleCardInfo())

                    if (amount > 1)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")
                }

                reporter.append("\n")
            }

            reporter.append("$totalRemovedCards cards have been removed from economy\n" +
                    "\n")

            if (mergedRemovedCards.isNotEmpty()) {
                reporter.append(
                    "Detailed information about removed cards\n" +
                            "\n"
                )

                mergedRemovedCards.forEach { (card, amount) ->
                    reporter.append(card.simpleCardInfo())

                    if (amount > 1)
                        reporter.append(" x").append(amount)

                    reporter.append("\n")
                }

                reporter.append("\n")
            }

            reporter.append(
                "$totalCatFoodFlow CF have been transferred among users via trading\n" +
                        "\n"
            )

            if (mergedCatFoodTrade.isNotEmpty()) {
                reporter.append("Detailed information about gained/lost cat food from trading\n" +
                        "\n")

                mergedCatFoodTrade.forEach { (id, amount) ->
                    val member = memberList.find { member -> member.idLong == id }

                    if (member != null) {
                        reporter.append(member.effectiveName).append(" [").append(member.id).append("] : ").append(amount).append("\n")
                    } else {
                        reporter.append("UNKNOWN USER [").append(id).append("] : ").append(amount).append("\n")
                    }
                }

                reporter.append("\n")
            }

            reporter.append("============================")

            val folder = File("./temp")

            if (!folder.exists() && !folder.mkdirs()) {
                StaticStore.logger.uploadLog("W/Report::doSomething - Failed to generate folder : ${folder.absolutePath}")

                return
            }

            val file = StaticStore.generateTempFile(folder, "log", ".txt", false)

            val writer = BufferedWriter(FileWriter(file, StandardCharsets.UTF_8))

            writer.write(reporter.toString())

            writer.close()

            sendMessageWithFile(ch, "Uploading full report log", file, "report.txt", loader.message)
        } else {
            val content = "Gathered ${sessions.size} log sessions in total before ${LogSession.globalFormat.format(time)}\n" +
                    "\n" +
                    "========== REPORT ==========\n" +
                    "\n" +
                    "${members.size} members participated BCTC\n" +
                    "\n" +
                    "Out of these people :\n" +
                    "\n" +
                    "$consumedCatFoodPack ${EmojiStore.ABILITY["CF"]?.formatted} have been consumed for generating pack\n" +
                    "\n" +
                    "$gainedCatFoodCraft ${EmojiStore.ABILITY["CF"]?.formatted} have been given out for compensation from crafting\n" +
                    "Users failed to craft T2 cards $totalCraftFailures times\n" +
                    "\n" +
                    "$totalGeneratedCards cards have been generated, and added into economy\n" +
                    "$totalRemovedCards cards have been removed from economy\n" +
                    "\n" +
                    "Summing above 2 data, " +
                    if (totalCards < 0)
                        "${-totalCards} cards have been removed from economy\n"
                    else
                        "$totalCards cards have been generated, and added into economy\n" +
                    "\n" +
                    "$totalCatFoodFlow ${EmojiStore.ABILITY["CF"]?.formatted} have been transferred among users via trading\n" +
                    "\n" +
                    "============================"

            replyToMessageSafely(ch, content, loader.message) { a -> a }
        }
    }

    private fun getSessionNumber(content: String) : Int {
        val contents = content.split(" ")

        contents.forEachIndexed { index, s ->
            if (s == "-n" && index < contents.size - 1 && StaticStore.isNumeric(contents[index + 1])) {
                return max(1, StaticStore.safeParseInt(contents[index + 1]))
            } else if (s == "-lf") {
                return -1
            }
        }

        return 30
    }
}