package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineNameModalHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineListHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Slot machine manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "create" -> {
                val input = TextInput.create("name", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setRequiredRange(1, 50)
                    .setPlaceholder("Decide Slot Machine Name")
                    .build()

                val modal = Modal.create("name", "Slot Machine Name").addComponents(Label.of("Name", input)).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineNameModalHolder(authorMessage, userID, channelID, message))
            }
            "slot" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val slotMachine = CardData.slotMachines[event.values[0].toInt()]

                connectTo(event, SlotMachineManageHolder(authorMessage, userID, channelID, message, slotMachine, false))
            }
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page += 10

                applyResult(event)
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed slot machine management!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onBack(child: Holder) {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.secondary("create", "Create Slot Machine").withEmoji(Emoji.fromUnicode("🎰"))))

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}