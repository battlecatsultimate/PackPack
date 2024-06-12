package mandarin.card.supporter.holder.slot

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.SlotMachineEntryFeeMinMaxHolder
import mandarin.card.supporter.slot.SlotEntryFee
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class SlotMachineEntryFeeHolder(author: Message, channelID: String, private val message: Message, private val slotMachine: SlotMachine) : ComponentHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "type" -> {
                val nextType = (slotMachine.entryFee.entryType.ordinal + 1) % SlotEntryFee.EntryType.entries.size

                slotMachine.entryFee.entryType = SlotEntryFee.EntryType.entries[nextType]

                if (slotMachine in CardData.slotMachines) {
                    CardBot.saveCardData()
                }

                val emoji = when(slotMachine.entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                }

                val entryType = when(slotMachine.entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
                }

                event.deferReply()
                    .setContent("Entry fee type is changed to $emoji $entryType! Check the result above")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "min" -> {
                val input = TextInput.create("entryFee", "Entry Fee", TextInputStyle.SHORT).setRequired(true).setPlaceholder("i.e. 1k -> 1000, 250k -> 250000").build()

                val modal = Modal.create("minMax", "Slot Machine Minimum Entry Fee")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineEntryFeeMinMaxHolder(authorMessage, channelID, message, slotMachine, true))
            }
            "max" -> {
                val input = TextInput.create("entryFee", "Entry Fee", TextInputStyle.SHORT).setRequired(true).setPlaceholder("i.e. 1k -> 1000, 250k -> 250000").build()

                val modal = Modal.create("minMax", "Slot Machine Maximum Entry Fee")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineEntryFeeMinMaxHolder(authorMessage, channelID, message, slotMachine, false))
            }
            "back" -> {
                goBack(event)
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, LangID.EN))
            }
        }
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val emoji = when (slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        val entryType = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
        }

        return "# ${slotMachine.name}\n" +
                "## Entry Fee Management Section\n" +
                "In this section, you can modify entry fee of this slot machine." +
                " Currently bot support ${EmojiStore.ABILITY["CF"]?.formatted} Cat Foods, or ${EmojiStore.ABILITY["SHARD"]?.formatted} Platinum Shards as entry fee." +
                " Once entry fee type is set, you can set minimum entryFee, and maximum entryFee." +
                " Users will be able to put dynamic entryFee." +
                " If they want risk, but big rewards, they will put more entryFee than minimum entryFee." +
                " If you don't want this slot machine to have ranged entry entryFee, set minimum entryFee and maximum entryFee same value each other\n" +
                "### Entry Fee Type\n" +
                "$emoji $entryType\n" +
                "### Entry Fee Info\n" +
                "Minimum Entry Fee : $emoji ${slotMachine.entryFee.minimumFee}\n" +
                "Maximum Entry Fee : $emoji ${slotMachine.entryFee.maximumFee}"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val emoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]
        }

        val entryType = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> "Cat Foods"
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> "Platinum Shards"
        }

        result.add(ActionRow.of(Button.secondary("type", "Entry Fee Type : $entryType").withEmoji(emoji)))

        result.add(ActionRow.of(
            Button.secondary("min", "Minimum Entry Fee").withEmoji(Emoji.fromUnicode("📤")),
            Button.secondary("max", "Maximum Entry Fee").withEmoji(Emoji.fromUnicode("📥"))
        ))

        if (slotMachine !in CardData.slotMachines) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
            ))
        }

        return result
    }
}