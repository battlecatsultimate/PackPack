package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.slot.SlotMachineSelectHolder
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
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

class Slot : Command(CommonStatic.Lang.Locale.EN, false){

    override fun doSomething(loader: CommandLoader) {
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
        
        val roles = m.roles.map { r -> r.idLong }
        val possibleSlotMachines = CardData.slotMachines.filter { s -> s.valid && s.activate && (s.roles.isEmpty() || s.roles.any { r -> r in roles }) }

        if (possibleSlotMachines.isEmpty()) {
            replyToMessageSafely(loader.channel, "There's no slot machine to roll... Contact card managers!", loader.message) { a -> a }

            return
        }

        val contents = loader.content.split(" ")

        val skip = "-s" in contents || "-skip" in contents

        replyToMessageSafely(loader.channel, "Select slot machine to roll", loader.message, { a -> a.setComponents(getComponents(loader.user, possibleSlotMachines)) }) { msg ->
            StaticStore.putHolder(loader.user.id, SlotMachineSelectHolder(loader.message, loader.user.id, loader.channel.id, msg, m, skip))
        }
    }

    private fun getComponents(user: User, possibleSlotMachines: List<SlotMachine>) : List<MessageTopLevelComponent> {
        val cooldownMap = CardData.slotCooldown.computeIfAbsent(user.idLong) { HashMap() }

        val currentTime = CardData.getUnixEpochTime()

        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        for (i in 0 until min(possibleSlotMachines.size, SearchHolder.PAGE_CHUNK)) {
            val cooldown = cooldownMap[possibleSlotMachines[i].uuid]

            var option = SelectOption.of(possibleSlotMachines[i].name, i.toString())

            if (cooldown != null && currentTime - cooldown < 0) {
                option = option.withDescription("Cooldown Left : ${CardData.convertMillisecondsToText(cooldown - currentTime)}")
            }

            options.add(option)
        }

        result.add(ActionRow.of(StringSelectMenu.create("slot").addOptions(options).setPlaceholder("Select Slot Machine To Roll").build()))

        if (possibleSlotMachines.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(possibleSlotMachines.size * 1.0 / SearchHolder.PAGE_CHUNK)

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

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}