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

class RemoveCC : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(loader.channel, "Format : `cd.rcc [User ID/Mention]`", loader.message) { a -> a }

            return
        }

        val value = contents[1].replace(Regex("(<@|>)"), "")

        if (!StaticStore.isNumeric(value)) {
            replyToMessageSafely(loader.channel, "User ID must be numeric!", loader.message) { a -> a }

            return
        }

        val id = StaticStore.safeParseLong(value)

        val atomicMember = AtomicReference<Member>(null)
        val countdown = CountDownLatch(1)

        loader.guild.retrieveMember(UserSnowflake.fromId(id)).queue({ targetMember ->
            atomicMember.set(targetMember)

            countdown.countDown()
        }) { _ ->
            countdown.countDown()
        }

        countdown.await()

        val member = atomicMember.get()

        if (member == null) {
            replyToMessageSafely(loader.channel, "Failed to retrieve member with ID of $id", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(loader.channel, "Are you sure you want to remove CC status from this user? This cannot be undone!", loader.message, { a -> registerConfirmButtons(a, lang)}) { msg ->
            StaticStore.putHolder(loader.user.id, ConfirmPopUpHolder(loader.message, loader.user.id, loader.channel.id, msg, { e ->
                val inventory = Inventory.getInventory(id)

                TransactionLogger.logCCCancel(id, loader.user.idLong, inventory)

                inventory.cancelCC(loader.guild, id)

                CardBot.saveCardData()

                val content = if (inventory.eccValidationWay != Inventory.ECCValidationWay.NONE) {
                    "Successfully removed CC/ECC from user <@$id>!"
                } else {
                    "Successfully removed CC from suer <@$id>"
                }

                e.deferEdit()
                    .setContent(content)
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()
            }, { e ->
                e.deferEdit()
                    .setContent("CC removal canceled")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()
            }, CommonStatic.Lang.Locale.EN))
        }
    }
}