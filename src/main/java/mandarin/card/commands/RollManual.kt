package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.utils.FileUpload
import java.util.concurrent.CountDownLatch
import kotlin.random.Random

class RollManual : Command(LangID.EN, true) {
    @Throws(Exception::class)
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val g = loader.guild

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val contents = loader.content.split(" ")

        if (contents.size < 3) {
            replyToMessageSafely(ch, "Not enough data! When you call this command, you have to provide user and " +
                    "which pack to be rolled. Format will be `${CardBot.globalPrefix}roll [User] [Pack]`\n\nUser can be provided via either " +
                    "ID or mention. For pack, call `-l` for large pack, `-s` for small pack, and `-p` for premium pack\n\nFor example, if you want" +
                    "to roll large pack for user A, then you have to call `p!roll @A -l`", loader.message
            ) { a -> a }

            return
        }

        val users = getUserID(loader.content, g)

        if (users.isEmpty()) {
            replyToMessageSafely(ch, "Bot failed to find user ID from command. User must be provided via either mention of user ID", loader.message) { a -> a }

            return
        }

        val pack = findPack(contents)

        if (pack == CardData.Pack.NONE) {
            replyToMessageSafely(ch, "Please specify which pack will be rolled. Pass `-l` for large pack, an pass `-s` for small pack", loader.message) { a -> a }

            return
        }

        try {
            if (users.size == 1) {
                g.retrieveMember(UserSnowflake.fromId(users[0])).queue { targetMember ->
                    replyToMessageSafely(ch, "\uD83C\uDFB2 Rolling...!", loader.message) { a -> a }

                    val result = rollCards(pack)

                    val inventory = Inventory.getInventory(targetMember.id)

                    try {
                        val builder = StringBuilder("### ${pack.getPackName()} Result [${result.size} cards in total]\n\n")

                        for (card in result) {
                            builder.append("- ")

                            if (card.tier == CardData.Tier.ULTRA) {
                                builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
                            } else if (card.tier == CardData.Tier.LEGEND) {
                                builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
                            }

                            builder.append(card.cardInfo())

                            if (!inventory.cards.containsKey(card)) {
                                builder.append(" {**NEW**}")
                            }

                            if (card.tier == CardData.Tier.ULTRA) {
                                builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
                            } else if (card.tier == CardData.Tier.LEGEND) {
                                builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                            }

                            builder.append("\n")
                        }

                        ch.sendMessage(builder.toString())
                            .setMessageReference(loader.message)
                            .mentionRepliedUser(false)
                            .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                            .queue()
                    } catch (_: Exception) {

                    }

                    inventory.addCards(result)

                    TransactionLogger.logRoll(result, pack, targetMember, true)
                }
            } else {
                users.forEach {
                    g.retrieveMember(UserSnowflake.fromId(it)).queue { targetMember ->
                        val result = rollCards(pack)

                        val inventory = Inventory.getInventory(targetMember.id)

                        inventory.addCards(result)

                        TransactionLogger.logRoll(result, pack, targetMember, true)
                    }
                }

                replyToMessageSafely(ch, "Rolled ${pack.getPackName()} for ${users.size} people successfully", loader.message) { a -> a }

                TransactionLogger.logMassRoll(m, users.size, pack)
            }
        } catch (_: Exception) {
            replyToMessageSafely(ch, "Bot failed to find provided user in this server", loader.message) { a -> a }
        }
    }

    private fun getUserID(contents: String, g: Guild) : List<String> {
        val result = ArrayList<String>()

        val segments = contents.split(Regex(" "), 2)

        if (segments.size < 2)
            return result

        val filtered = segments[1].replace(Regex("-[slp]"), "").replace(" ", "").split(Regex(","))

        for(segment in filtered) {
            if (StaticStore.isNumeric(segment)) {
                result.add(segment)
            } else if (segment.startsWith("<@")) {
                result.add(segment.replace("<@", "").replace(">", ""))
            }
        }

        result.removeIf { id ->
            val waiter = CountDownLatch(1)
            var needRemoval = false

            g.retrieveMember(UserSnowflake.fromId(id)).queue({ _ ->
                waiter.countDown()
                needRemoval = false
            }, { _ ->
                waiter.countDown()
                needRemoval = true
            })

            waiter.await()

            needRemoval
        }

        return result
    }

    private fun findPack(contents: List<String>) : CardData.Pack {
        for(segment in contents) {
            when(segment) {
                "-s" -> return CardData.Pack.SMALL
                "-l" -> return CardData.Pack.LARGE
                "-p" -> return CardData.Pack.PREMIUM
            }
        }

        return CardData.Pack.NONE
    }

    private fun rollCards(pack: CardData.Pack) : List<Card> {
        val result = ArrayList<Card>()

        when(pack) {
            CardData.Pack.SMALL -> {
                repeat(4) {
                    result.add(CardData.common.random())
                }

                val chance = Random.nextDouble()

                if (chance <= 0.7) {
                    result.add(CardData.common.random())
                } else {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                }
            }
            CardData.Pack.LARGE -> {
                repeat(8) {
                    result.add(CardData.common.random())
                }

                var chance = Random.nextDouble()

                if (chance <= 0.5) {
                    result.add(CardData.common.random())
                } else {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                }

                chance = Random.nextDouble()

                if (chance <= 0.99) {
                    result.add(CardData.appendUncommon(CardData.uncommon).random())
                } else {
                    result.add(CardData.appendUltra(CardData.ultraRare).random())
                }
            }
            CardData.Pack.PREMIUM -> {
                repeat(5) {
                    val chance = Random.nextDouble()

                    if (chance <= 0.93) {
                        result.add(CardData.common.random())
                    } else if (chance <= 0.995) {
                        result.add(CardData.appendUltra(CardData.ultraRare).random())
                    } else {
                        result.add(CardData.appendLR(CardData.legendRare).random())
                    }
                }
            }
            else -> {
                throw IllegalStateException("Invalid pack type $pack found")
            }
        }

        return result
    }
}
