package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.ManualSlotSelectHolder
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.concurrent.CountDownLatch
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineManual : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        if (loader.user.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(loader.member)) {
            return
        }

        val possibleSlotMachines = CardData.slotMachines.filter { s -> s.valid }

        if (possibleSlotMachines.isEmpty()) {
            replyToMessageSafely(loader.channel, "There's no slot machine to roll...", loader.message) { a -> a }

            return
        }

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(loader.channel, "Not enough data! You have to pass members data to manually roll the slot machine!", loader.message
            ) { a -> a }

            return
        }

        val users = getUserID(loader.content, loader.guild)

        if (users.isEmpty()) {
            replyToMessageSafely(loader.channel, "Bot failed to find user ID from command. User must be provided via either mention of user ID", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(loader.channel, "Select slot machine to roll", loader.message, { a -> a.setComponents(getComponents(possibleSlotMachines)) }) { msg ->
            StaticStore.putHolder(loader.user.id, ManualSlotSelectHolder(loader.message, loader.user.id, loader.channel.id, msg, loader.member, users))
        }
    }

    private fun getUserID(contents: String, g: Guild) : List<String> {
        val result = ArrayList<String>()

        val segments = contents.split(Regex(" "), 2)

        if (segments.size < 2)
            return result

        val filtered = segments[1].replace(" ", "").split(Regex(","))

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

            try {
                g.retrieveMember(UserSnowflake.fromId(id)).queue({ _ ->
                    waiter.countDown()
                    needRemoval = false
                }, { _ ->
                    waiter.countDown()
                    needRemoval = true
                })
            } catch (_: IllegalArgumentException) {
                needRemoval = true
            }

            waiter.await()

            needRemoval
        }

        return result
    }

    private fun getComponents(possibleSlotMachines: List<SlotMachine>) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        for (i in 0 until min(possibleSlotMachines.size, SearchHolder.PAGE_CHUNK)) {
            val option = SelectOption.of(possibleSlotMachines[i].name, i.toString())

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