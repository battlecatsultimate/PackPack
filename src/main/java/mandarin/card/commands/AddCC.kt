package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class AddCC : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val g = loader.guild
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val contents = loader.content.split(" ")

        val id = getMemberID(contents)

        if (id == -1L) {
            replyToMessageSafely(loader.channel, "Format : `${CardBot.globalPrefix}acc [Member ID/Mention] -r [Reason]`", loader.message) { a -> a }

            return
        }

        val atomicMember = AtomicReference<Member>()
        val countdown = CountDownLatch(1)

        g.retrieveMember(UserSnowflake.fromId(id)).queue({ m ->
            atomicMember.set(m)
            countdown.countDown()
        }, { e ->
            StaticStore.logger.uploadErrorLog(e, "E/AddCC::doSomething - Failed to retrieve member with ID of $id")

            countdown.countDown()
        })

        countdown.await()

        if (atomicMember.get() == null) {
            replyToMessageSafely(loader.channel, "Failed to retrieve member with ID of $id. Maybe incorrect ID?", loader.message) { a -> a }

            return
        }

        val inventory = Inventory.getInventory(id)

        if (inventory.ccValidationWay != Inventory.CCValidationWay.NONE) {
            replyToMessageSafely(loader.channel, "It seems that this user already has CC. Please guide user to cancel their CC first", loader.message) { a -> a }

            return
        }

        val reason = getReason(contents)

        replyToMessageSafely(loader.channel, "Are you sure you want to give <@$id> [$id] CC? Check below\n\nCC Validation Way : MANUAL\nReason : $reason", loader.message, { a -> registerConfirmButtons(a, lang) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmPopUpHolder(loader.message, m.id, loader.channel.id, msg, { e ->
                inventory.ccValidationWay = Inventory.CCValidationWay.MANUAL
                inventory.ccValidationReason = reason
                inventory.ccValidationTime = CardData.getUnixEpochTime()

                CardBot.saveCardData()

                val role = g.roles.find { role -> role.id == CardData.cc }

                if (role != null) {
                    g.addRoleToMember(UserSnowflake.fromId(id), role).queue()
                }

                TransactionLogger.logCCAdd(id, m.idLong, inventory)

                e.deferEdit()
                    .setContent("Successfully gave CC to user <@$id> [$id] with reason of `Manual : $reason`!")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()
            }, { e ->
                e.deferEdit()
                    .setContent("ECC giving canceled")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()
            }, lang))
        }
    }

    private fun getReason(contents: List<String>) : String {
        val builder = StringBuilder()

        var build = false

        contents.forEach { content ->
            if (content == "-r" && !build) {
                build = true
            } else if (build) {
                builder.append(content).append(" ")
            }
        }

        return builder.toString().trim()
    }

    private fun getMemberID(contents: List<String>) : Long {
        contents.forEach { content ->
            if (content == "-r") {
                return -1L
            }

            if (StaticStore.isNumeric(content) || content.matches(Regex("<@\\d+>"))) {
                return StaticStore.safeParseLong(content.replace(Regex("<@|>"), ""))
            }
        }

        return -1L
    }
}