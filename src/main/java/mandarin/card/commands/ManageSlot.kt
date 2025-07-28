package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.slot.SlotMachineListHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.data.ConfigHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.emoji.Emoji
import kotlin.math.ceil
import kotlin.math.min

class ManageSlot : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.addComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(loader.user.id, SlotMachineListHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    private fun getContents() : String {
        val builder = StringBuilder(
            "# Slot Machine Manager\n" +
                "You can manage slot machines here. Click `Create Slot Machine` button to add new slot machine. Select slot machine in the list to modify or delete it\n" +
                "## List of Slot Machines\n"
        )

        if (CardData.slotMachines.isEmpty()) {
            builder.append("- No Slot Machine")
        } else {
            val size = min(CardData.slotMachines.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize)

            for (i in 0 until size) {
                builder.append(i + 1).append(". ").append(CardData.slotMachines[i].name)

                if (i < size - 1) {
                    builder.append("\n")
                }
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        if (CardData.slotMachines.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in 0 until min(CardData.slotMachines.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                options.add(SelectOption.of(CardData.slotMachines[i].name, i.toString()))
            }
        }

        val selectMenuBuilder = StringSelectMenu.create("slot").addOptions(options)

        if (CardData.slotMachines.isEmpty()) {
            selectMenuBuilder.setPlaceholder("No Slot Machine to Select").setDisabled(true)
        } else {
            selectMenuBuilder.setPlaceholder("Select Slot Machine to Modify")
        }

        result.add(ActionRow.of(selectMenuBuilder.build()))

        if (CardData.slotMachines.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(CardData.slotMachines.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize)

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(true))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(true))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.secondary("create", "Create Slot Machine").withEmoji(Emoji.fromUnicode("🎰"))))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}