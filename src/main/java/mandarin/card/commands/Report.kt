package mandarin.card.commands

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import kotlin.math.max

class Report : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent) {
        val m = getMember(event) ?: return
        val ch = getChannel(event) ?: return

        if (!CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val time = CardData.getUnixEpochTime()

        val sessionNumber = getSessionNumber(getContent(event))

        val sessions = LogSession.gatherPreviousSessions(time, sessionNumber)

        val members = HashSet<Long>()

        sessions.forEach { session ->
            members.addAll(session.activeMembers)
        }

        val consumedCatFoodPack = sessions.sumOf { session -> session.catFoodPack.values.sum() }

        val gainedCatFoodCraft = sessions.sumOf { session -> session.catFoodCraft.values.sum() }

        val totalTier2Cards = sessions.sumOf { session -> session.tier2Cards.size }
        val totalCraftFailures = sessions.sumOf { session -> session.craftFailures }

        val totalGeneratedCards = sessions.sumOf { session -> session.generatedCards.size }
        val totalRemovedCards = sessions.sumOf { session -> session.removedCards.size }

        val totalCatFoodFlow = sessions.sumOf { session -> session.catFoodTradeSum }

        if (getContent(event).contains("-f")) {
            val mergedCatFoodPack = HashMap<Long, Long>()
            val mergedCatFoodCraft = HashMap<Long, Long>()
            val mergedCatFoodTrade = HashMap<Long, Long>()

            val mergedTier2Cards = HashMap<Card, Long>()

            val mergedGeneratedCards = HashMap<Card, Long>()
            val mergedRemovedCards = HashMap<Card, Long>()

            sessions.forEach { session ->
                session.catFoodPack.forEach { (id, amount) ->
                    mergedCatFoodPack[id] = (mergedCatFoodPack[id] ?: 0) + amount
                }

                session.catFoodCraft.forEach { (id, amount) ->
                    mergedCatFoodCraft[id] = (mergedCatFoodCraft[id] ?: 0) + amount
                }

                session.catFoodTrade.forEach { (id, amount) ->
                    mergedCatFoodTrade[id] = (mergedCatFoodTrade[id] ?: 0) + amount
                }

                session.tier2Cards.forEach { card ->
                    mergedTier2Cards[card] = (mergedTier2Cards[card] ?: 0) + 1
                }

                session.generatedCards.forEach { (card, amount) ->
                    mergedGeneratedCards[card] = (mergedGeneratedCards[card] ?: 0) + amount
                }

                session.removedCards.forEach { (card, amount) ->
                    mergedRemovedCards[card] = (mergedRemovedCards[card] ?: 0) + amount
                }
            }

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

            event.guild.retrieveMembers(members.map { id -> UserSnowflake.fromId(id) })
                .onSuccess { list ->
                    list.forEach { member ->
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
                            val member = list.find { member -> member.idLong == id }

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
                            val member = list.find { member -> member.idLong == id }

                            if (member != null) {
                                reporter.append(member.effectiveName).append(" [").append(member.id).append("] : ").append(amount).append("\n")
                            } else {
                                reporter.append("UNKNOWN USER [").append(id).append("] : ").append(amount).append("\n")
                            }
                        }

                        reporter.append("\n")
                    }

                    reporter.append(
                        "$totalTier2Cards T2 cards have been generated from crafting\n" +
                                "Users failed to craft T2 cards $totalCraftFailures times\n" +
                                "\n"
                    )

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

                        mergedGeneratedCards.toSortedMap(CardComparator()).forEach { (card, amount) ->
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

                        mergedRemovedCards.toSortedMap(CardComparator()).forEach { (card, amount) ->
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
                            val member = list.find { member -> member.idLong == id }

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

                        return@onSuccess
                    }

                    val file = StaticStore.generateTempFile(folder, "log", ".txt", false)

                    val writer = BufferedWriter(FileWriter(file, StandardCharsets.UTF_8))

                    writer.write(reporter.toString())

                    writer.close()

                    sendMessageWithFile(ch, "Uploading full report log", file, "report.txt", getMessage(event))
                }
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
                    "$totalTier2Cards T2 cards have been generated from crafting\n" +
                    "Users failed to craft T2 cards $totalCraftFailures times\n" +
                    "\n" +
                    "$totalGeneratedCards cards have been generated, and added into economy\n" +
                    "$totalRemovedCards cards have been removed from economy\n" +
                    "\n" +
                    "$totalCatFoodFlow ${EmojiStore.ABILITY["CF"]?.formatted} have been transferred among users via trading\n" +
                    "\n" +
                    "============================"

            replyToMessageSafely(ch, content, getMessage(event)) { a -> a }
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