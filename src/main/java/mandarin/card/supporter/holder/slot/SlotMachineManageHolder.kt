package mandarin.card.supporter.holder.slot

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.SlotMachineCooldownModalHolder
import mandarin.card.supporter.holder.modal.SlotMachineNameModalHolder
import mandarin.card.supporter.holder.modal.SlotMachineSlotSizeModalHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class SlotMachineManageHolder(author: Message, channelID: String, private val message: Message, private val slotMachine: SlotMachine, private val new: Boolean) : ComponentHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "slot" -> {
                val input = TextInput.create("slot", "Size", TextInputStyle.SHORT).setRequired(true).build()

                val modal = Modal.create("size", "Slot Machine Slot Size")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineSlotSizeModalHolder(authorMessage, channelID, message, slotMachine))
            }
            "name" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT).setPlaceholder("Decide Slot Machine Name Here").setRequired(true).setRequiredRange(1, 50).build()

                val modal = Modal.create("name", "Slot Machine Name").addActionRow(input).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineNameModalHolder(authorMessage, channelID, message, slotMachine))
            }
            "entryFee" -> {
                connectTo(event, SlotMachineEntryFeeHolder(authorMessage, channelID, message, slotMachine))
            }
            "content" -> {
                connectTo(event, SlotMachineContentHolder(authorMessage, channelID, message, slotMachine))
            }
            "cooldown" -> {
                val input = TextInput.create("cooldown", "Cooldown", TextInputStyle.SHORT).setRequired(true).setPlaceholder("i.e. 3d4h30m -> 3 Days 4 Hours 30 Minutes").build()

                val modal = Modal.create("cooldown", "Slot Machine Cooldown")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineCooldownModalHolder(authorMessage, channelID, message, slotMachine))
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
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, LangID.EN))
            }
            "back" -> {
                slotMachine.activate = slotMachine.valid

                goBack(event)
            }
            "activate" -> {
                slotMachine.activate = !slotMachine.activate

                CardBot.saveCardData()

                applyResult(event)
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this slot machine? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    CardData.slotMachines.remove(slotMachine)

                    CardBot.saveCardData()

                    TransactionLogger.logSlotMachineDeletion(authorMessage.author.idLong, slotMachine)

                    e.deferReply().setContent("Successfully removed slot machine : ${slotMachine.name}!").setEphemeral(true).queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, LangID.EN))
            }
        }
    }

    override fun onConnected(event: ModalInteractionEvent) {
        applyResult(event)
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack() {
        applyResult()
    }

    private fun applyResult() {
        message.editMessage(slotMachine.asText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(slotMachine.asText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: ModalInteractionEvent) {
        event.deferEdit()
            .setContent(slotMachine.asText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("name", "Change Slot Machine Name").withEmoji(Emoji.fromUnicode("🏷️")),
            Button.secondary("slot", "Change Slot Size").withEmoji(Emoji.fromUnicode("🍀"))
        ))
        result.add(ActionRow.of(
            Button.secondary("entryFee", "Entry Fee").withEmoji(Emoji.fromUnicode("💵")),
            Button.secondary("content", "Slot Contents").withEmoji(Emoji.fromUnicode("🎰"))
        ))
        result.add(ActionRow.of(Button.secondary("cooldown", "Cooldown").withEmoji(Emoji.fromUnicode("⏰"))))

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