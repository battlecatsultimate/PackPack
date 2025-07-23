package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.pack.PackSelectHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil
import kotlin.math.min

class Roll : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = if (loader.hasGuild()) {
            loader.member
        } else {
            val u = loader.user
            val g = loader.client.getGuildById(CardData.guild) ?: return

            val retriever = AtomicReference<Member>(null)
            val countdown = CountDownLatch(1)

            g.retrieveMember(u).queue({ m ->
                retriever.set(m)

                countdown.countDown()
            }) { e ->
                StaticStore.logger.uploadErrorLog(e, "E/Craft::doSomething - Failed to retrieve member data from user ID ${u.idLong}")

                countdown.countDown()
            }

            countdown.await()

            retriever.get() ?: return
        }

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val packList = CardData.cardPacks.filter { pack -> pack.activated && !pack.isInvalid() && (pack.cost.roles.isEmpty() || pack.cost.roles.any { id -> id in m.roles.map { role -> role.id } }) }

        if (packList.isEmpty()) {
            replyToMessageSafely(ch, "Umm... There's no card pack to roll, please contact card managers", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Please select the pack that you want to roll", loader.message, { a ->
            a.setComponents(registerComponents(m, packList))
        }, { msg ->
            val content = loader.content.split(" ")

            val noImage = arrayOf("-s", "-simple", "-n", "-noimage").any { p -> p in content }

            StaticStore.putHolder(m.id, PackSelectHolder(loader.message, m.id, ch.id, m, msg, noImage))
        })
    }

    private fun registerComponents(member: Member, packList: List<CardPack>) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val packOptions = ArrayList<SelectOption>()

        val size = min(packList.size, SearchHolder.PAGE_CHUNK)

        for (i in 0 until size) {
            val pack = packList[i]
            val cooldownMap = CardData.cooldown[member.idLong]

            val desc = if (cooldownMap == null) {
                ""
            } else {
                val cooldown = cooldownMap[pack.uuid]

                if (cooldown != null && cooldown - CardData.getUnixEpochTime() > 0) {
                    "Cooldown Left : ${CardData.convertMillisecondsToText(cooldown - CardData.getUnixEpochTime())}"
                } else {
                    ""
                }
            }

            packOptions.add(
                SelectOption.of(pack.packName, pack.uuid).withDescription(desc.ifEmpty { null }).withEmoji(EmojiStore.getPackEmoji(pack))
            )
        }

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

        if (packList.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(packList.size * 1.0 / SearchHolder.PAGE_CHUNK)

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.danger("close", "Close")))

        return result
    }
}