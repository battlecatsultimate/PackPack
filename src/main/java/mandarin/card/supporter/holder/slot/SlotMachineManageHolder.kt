package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineCooldownModalHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineNameModalHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineSlotSizeModalHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class SlotMachineManageHolder(author: Message, userID: String, channelID: String, message: Message, private val slotMachine: SlotMachine, private val new: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "slot" -> {
                val input = TextInput.create("slot", "Size", TextInputStyle.SHORT).setRequired(true).build()

                val modal = Modal.create("size", "Slot Machine Slot Size")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineSlotSizeModalHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "roles" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                event.values.forEach { id ->
                    if (id.idLong in slotMachine.roles) {
                        slotMachine.roles.remove(id.idLong)
                    } else {
                        slotMachine.roles.add(id.idLong)
                    }
                }

                event.deferReply().setContent("Successfully added/removed selected roles! Check the result above").setEphemeral(true).queue()

                applyResult()
            }
            "removeRole" -> {
                slotMachine.roles.clear()

                event.deferReply().setContent("Successfully removed all roles! Check the result above").setEphemeral(true).queue()

                applyResult()
            }
            "name" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT).setPlaceholder("Decide Slot Machine Name Here").setRequired(true).setRequiredRange(1, 50).build()

                val modal = Modal.create("name", "Slot Machine Name").addComponents(ActionRow.of(input)).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineNameModalHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "entryFee" -> {
                connectTo(event, SlotMachineEntryFeeHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "content" -> {
                connectTo(event, SlotMachineContentHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "cooldown" -> {
                val input = TextInput.create("cooldown", "Cooldown", TextInputStyle.SHORT).setRequired(true).setPlaceholder("i.e. 3d4h30m -> 3 Days 4 Hours 30 Minutes").build()

                val modal = Modal.create("cooldown", "Slot Machine Cooldown")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineCooldownModalHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "create" -> {
                CardData.slotMachines.add(slotMachine)

                CardBot.saveCardData()

                TransactionLogger.logSlotMachineCreation(authorMessage.author.idLong, slotMachine)

                event.deferReply()
                    .setContent("Successfully created slot machine! Check the list above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
            "prev" -> {
                page--

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "back" -> {
                slotMachine.activate = slotMachine.activate && slotMachine.valid

                goBack(event)
            }
            "activate" -> {
                slotMachine.activate = !slotMachine.activate

                CardBot.saveCardData()

                applyResult(event)
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    CardData.slotMachines.remove(slotMachine)

                    CardBot.saveCardData()

                    TransactionLogger.logSlotMachineDeletion(authorMessage.author.idLong, slotMachine)

                    e.deferReply().setContent("Successfully removed slot machine : ${slotMachine.name}!").setEphemeral(true).queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult() {
        message.editMessage(slotMachine.asText(page))
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(slotMachine.asText(page))
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            Button.secondary("name", "Change Slot Machine Name").withEmoji(Emoji.fromUnicode("🏷️")),
            Button.secondary("slot", "Change Slot Size").withEmoji(Emoji.fromUnicode("🍀")),
            Button.secondary("entryFee", "Entry Fee").withEmoji(Emoji.fromUnicode("💵"))
        ))

        result.add(ActionRow.of(
            Button.secondary("content", "Slot Contents").withEmoji(Emoji.fromUnicode("🎰")),
            Button.secondary("cooldown", "Cooldown").withEmoji(Emoji.fromUnicode("⏰"))
        ))

        if (slotMachine.content.size > SlotMachine.PAGE_CHUNK) {
            val totalPage = ceil(slotMachine.content.size * 1.0 / SlotMachine.PAGE_CHUNK).toInt()

            result.add(ActionRow.of(
                Button.secondary("prev", "Previous Rewards").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next Rewards").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage)
            ))
        }

        result.add(ActionRow.of(
            EntitySelectMenu.create("roles", EntitySelectMenu.SelectTarget.ROLE).setPlaceholder("Select Role To Add/Delete").setRequiredRange(0, SelectMenu.OPTIONS_MAX_AMOUNT).build()
        ))

        result.add(ActionRow.of(
            Button.secondary("removeRole", "Remove All Roles").withDisabled(slotMachine.roles.isEmpty())
        ))

        if (new) {
            result.add(ActionRow.of(
                Button.success("create", "Create").withEmoji(EmojiStore.CHECK).withDisabled(!slotMachine.valid),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK).withDisabled(!slotMachine.valid),
                Button.secondary("activate", "Activate").withEmoji(if (slotMachine.activate) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF).withDisabled(!slotMachine.valid),
                Button.danger("delete", "Delete").withEmoji(EmojiStore.CROSS)
            ))
        }

        return result
    }
}