package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.YDKEValidator
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class YDKE : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val contents = loader.content.split(" ")

        if (contents.size < 4) {
            replyToMessageSafely(loader.channel, "This command requires 3 parameters : `p!ydke [-n|-t] [Member ID/Mention] [YDKE Link]`\n\n`-n` : Normal white list\n`-t` : BCTC-T (Tournament) white list", loader.message) { a -> a }

            return
        }

        if (!contents[1].matches(Regex("-[nt]"))) {
            replyToMessageSafely(loader.channel, "You have to provide proper white list parameter!\n\n`-n` : Normal white list\n`-t` : BCTC-T (Tournament) white list", loader.message) { a -> a }

            return
        }

        if (!StaticStore.isNumeric(contents[2]) && !contents[2].matches(Regex("<@\\d+>"))) {
            replyToMessageSafely(loader.channel, "Please provide proper member ID or mention!", loader.message) { a -> a }

            return
        }

        if (!contents[3].matches(Regex("ydke://[0-9a-zA-Z/=+]*![0-9a-zA-Z/=+]*![0-9a-zA-Z/=+]*!?"))) {
            replyToMessageSafely(loader.channel, "Invalid YDKE link format detected. Please check the link!", loader.message) { a -> a }

            return
        }

        val whiteList = when(contents[1]) {
            "-n" -> YDKEValidator.WhiteList.NORMAL
            "-t" -> YDKEValidator.WhiteList.TOURNAMENT
            else -> YDKEValidator.WhiteList.BCE
        }

        val id = StaticStore.safeParseLong(contents[2].replace(Regex("<@|>"), ""))

        val countdown = CountDownLatch(1)
        val atomicMember = AtomicReference<Member>()

        loader.guild.retrieveMember(UserSnowflake.fromId(id)).queue({ member ->
            atomicMember.set(member)

            countdown.countDown()
        }) { _ ->
            countdown.countDown()
        }

        countdown.await()

        if (atomicMember.get() == null) {
            replyToMessageSafely(loader.channel, "Failed to find member with ID of $id in this server...", loader.message) { a -> a }

            return
        }

        val inventory = Inventory.getInventory(id)

        val sanitizedResult = YDKEValidator.sanitize(inventory, contents[3], whiteList)

        val sanitizedLink = sanitizedResult.first
        val reasons = sanitizedResult.second

        val data = YDKEValidator.toData(sanitizedLink)

        val t0Main = findT0CardMap(data, YDKEValidator.MAIN)
        val t0Extra = findT0CardMap(data, YDKEValidator.EXTRA)
        val t0Side = findT0CardMap(data, YDKEValidator.SIDE)

        val lines = ArrayList<String>()

        if (reasons.isNotEmpty()) {
            lines.add("There was conflict between the YDKE link and this user's inventory. Bot modified the YDKE link")
            lines.add("## Modification Log")

            val mainReason = reasons.filter { reason -> reason.startsWith("Main : ") }.map { reason -> reason.replace("Main : ", "") }
            val extraReason = reasons.filter { reason -> reason.startsWith("Extra : ") }.map { reason -> reason.replace("Extra : ", "") }
            val sideReason = reasons.filter { reason -> reason.startsWith("Side : ") }.map { reason -> reason.replace("Side : ", "") }

            if (mainReason.isNotEmpty()) {
                lines.add("### Main")
                lines.add("```")

                mainReason.forEachIndexed { index, line ->
                    lines.add(line)

                    if (index < mainReason.lastIndex) {
                        lines.add("\n")
                    }
                }

                lines.add("```")
            }

            if (extraReason.isNotEmpty()) {
                lines.add("### Extra")
                lines.add("```")

                extraReason.forEachIndexed { index, line ->
                    lines.add(line)

                    if (index < extraReason.lastIndex) {
                        lines.add("\n")
                    }
                }

                lines.add("```")
            }

            if (sideReason.isNotEmpty()) {
                lines.add("### Side")
                lines.add("```")

                sideReason.forEachIndexed { index, line ->
                    lines.add(line)

                    if (index < sideReason.lastIndex) {
                        lines.add("\n")
                    }
                }

                lines.add("```")
            }
        } else {
            lines.add("The link was clean and synced with this user's inventory!")
        }

        if (t0Main.any { it.isNotEmpty() } || t0Extra.any { it.isNotEmpty() } || t0Side.any { it.isNotEmpty() }) {
            lines.add("## T0 Log from YDKE link")

            if (t0Main.any { it.isNotEmpty() }) {
                lines.add("### Main")

                t0Main.forEachIndexed { index, map ->
                    if (map.isEmpty())
                        return@forEachIndexed

                    lines.add(when(index) {
                        0 -> "- **Normal T0**"
                        1 -> "- **Enemy T0**"
                        2 -> "- **Custom Enemy T0**"
                        else -> "- **Custom T0**"
                    })

                    var number = 1

                    map.keys.sorted().forEach { key ->
                        val amount = map[key] ?: return@forEach

                        if (amount > 2)
                            lines.add("$number. $key x$amount")
                        else
                            lines.add("$number. $key")

                        number++
                    }
                }
            }

            if (t0Extra.any { it.isNotEmpty() }) {
                lines.add("### Extra")

                t0Extra.forEachIndexed { index, map ->
                    if (map.isEmpty())
                        return@forEachIndexed

                    lines.add(when(index) {
                        0 -> "- **Normal T0**"
                        1 -> "- **Enemy T0**"
                        2 -> "- **Custom Enemy T0**"
                        else -> "- **Custom T0**"
                    })

                    var number = 1

                    map.keys.sorted().forEach { key ->
                        val amount = map[key] ?: return@forEach

                        if (amount > 2)
                            lines.add("$number. $key x$amount")
                        else
                            lines.add("$number. $key")

                        number++
                    }
                }
            }

            if (t0Side.any { it.isNotEmpty() }) {
                lines.add("### Side")

                t0Side.forEachIndexed { index, map ->
                    if (map.isEmpty())
                        return@forEachIndexed

                    lines.add(when(index) {
                        0 -> "- **Normal T0**"
                        1 -> "- **Enemy T0**"
                        2 -> "- **Custom Enemy T0**"
                        else -> "- **Custom T0**"
                    })

                    var number = 1

                    map.keys.sorted().forEach { key ->
                        val amount = map[key] ?: return@forEach

                        if (amount > 2)
                            lines.add("$number. $key x$amount")
                        else
                            lines.add("$number. $key")

                        number++
                    }
                }
            }
        }

        lines.add("## Final Link")
        lines.add("```")
        lines.add(sanitizedLink)
        lines.add("```")

        lines.add("## Final Deck Size")
        lines.add("```")
        lines.add("Main  : ${data[0].size}")
        lines.add("Extra : ${data[1].size}")
        lines.add("Side  : ${data[2].size}")
        lines.add("```")

        val builder = StringBuilder()
        var replied = false
        var codeBlock = false

        lines.forEach { line ->
            if (line == "```") {
                codeBlock = !codeBlock
            }

            if (builder.length + line.length + 1 >= 1900) {
                if (codeBlock) {
                    builder.append("```")
                }

                if (replied) {
                    loader.channel.sendMessage(builder.toString()).setAllowedMentions(arrayListOf()).queue()
                } else {
                    replyToMessageSafely(loader.channel, builder.toString(), loader.message) { a -> a }

                    replied = true
                }

                builder.clear()

                if (codeBlock) {
                    builder.append("```\n")
                }
            }

            builder.append(line).append("\n")
        }

        if (replied) {
            loader.channel.sendMessage(builder.toString()).setAllowedMentions(arrayListOf()).queue()
        } else {
            replyToMessageSafely(loader.channel, builder.toString(), loader.message) { a -> a }
        }
    }

    private fun findT0CardMap(data: ArrayList<ArrayList<Long>>, index: Int) : List<Map<Long, Int>> {
        val result = ArrayList<Map<Long, Int>>()

        val t0 = arrayOf(
            data[index].filter { value -> value in 210661000 until 210662000 },
            data[index].filter { value -> value in 210662000 until 210662900 },
            data[index].filter { value -> value in 210662900 until 210663000 },
            data[index].filter { value -> value in 210663000 until 210664000 }
        )

        t0.forEach { values ->
            val cardMap = HashMap<Long, Int>()

            values.forEach { value ->
                cardMap[value] = (cardMap[value] ?: 0) + 1
            }

            result.add(cardMap)
        }

        return result
    }
}