package mandarin.card.supporter.holder.slot

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.slot.SlotMachineRollModalHolder
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class SlotMachineConfirmHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine, private val skip: Boolean) : ComponentHolder(author, channelID, message) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "roll" -> {
                if (slotMachine.entryFee.minimumFee == slotMachine.entryFee.maximumFee) {
                    val entryEmoji = when(slotMachine.entryFee.entryType) {
                        SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                        SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                    }

                    val fee = slotMachine.entryFee.maximumFee

                    registerPopUp(event, "Are you sure you want to roll this slot machine with entry fee of $entryEmoji $fee?", LangID.EN)

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { ev ->
                        ev.deferEdit()
                            .setContent("Rolling...! 🎲")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue {
                                slotMachine.roll(message, authorMessage.author.idLong, inventory, fee, skip)
                            }

                        expired = true
                    }, LangID.EN))
                } else {
                    val minimumInput = max(slotMachine.entryFee.minimumFee, 1)

                    val maximumInput = when(slotMachine.entryFee.entryType) {
                        SlotEntryFee.EntryType.CAT_FOOD -> min(slotMachine.entryFee.maximumFee, inventory.actualCatFood)
                        SlotEntryFee.EntryType.PLATINUM_SHARDS -> min(slotMachine.entryFee.maximumFee, inventory.platinumShard)
                    }

                    val input = TextInput.create("fee", "Entry Fee", TextInputStyle.SHORT).setPlaceholder("Put Entry Fee From $minimumInput To $maximumInput").setRequired(true).build()

                    val modal = Modal.create("roll", "Slot Machine Roll").addActionRow(input).build()

                    event.replyModal(modal).queue()

                    connectTo(
                        SlotMachineRollModalHolder(
                            authorMessage,
                            channelID,
                            message,
                            slotMachine,
                            inventory
                        ) { e, fee ->
                            val entryEmoji = when (slotMachine.entryFee.entryType) {
                                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                            }

                            registerPopUp(
                                e,
                                "Are you sure you want to roll this slot machine with entry fee of $entryEmoji $fee?",
                                LangID.EN
                            )

                            connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { ev ->
                                ev.deferEdit()
                                    .setContent("Rolling...! 🎲")
                                    .setComponents()
                                    .setAllowedMentions(ArrayList())
                                    .mentionRepliedUser(false)
                                    .queue {
                                        slotMachine.roll(message, authorMessage.author.idLong, inventory, fee, skip)
                                    }

                                expired = true
                            }, LangID.EN))
                        })
                }
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
                goBack(event)
            }
        }
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        var result = slotMachine.asText(page) + "\n"

        val currentTime = CardData.getUnixEpochTime()
        val cooldownMap = CardData.slotCooldown.computeIfAbsent(authorMessage.author.id) { HashMap() }
        val time = cooldownMap[slotMachine.uuid] ?: 0L

        if (!slotMachine.valid) {
            result += "\n- This slot machine contains invalid condition! Please contact card managers"
        }

        if (time > currentTime) {
            result += "\n- This slot machine's cooldown hasn't ended yet! Currently `${CardData.convertMillisecondsToText(time - currentTime)}` left"
        }

        val canRoll = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> inventory.actualCatFood >= slotMachine.entryFee.minimumFee
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard >= slotMachine.entryFee.minimumFee
        }

        val entryEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        val currentValue = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> inventory.actualCatFood
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard
        }

        if (!canRoll) {
            result += "\n- You can't roll this slot machine. It requires at least $entryEmoji ${slotMachine.entryFee.minimumFee}, and you currently have $entryEmoji $currentValue!"
        }

        return result
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val canRoll = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> inventory.actualCatFood >= slotMachine.entryFee.minimumFee
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> inventory.platinumShard >= slotMachine.entryFee.minimumFee
        }

        val currentTime = CardData.getUnixEpochTime()
        val cooldownMap = CardData.slotCooldown.computeIfAbsent(authorMessage.author.id) { HashMap() }
        val time = cooldownMap[slotMachine.uuid] ?: 0L

        val timeEnded = currentTime >= time

        result.add(ActionRow.of(
            Button.secondary("roll", "Roll!").withEmoji(Emoji.fromUnicode("🎰")).withDisabled(!slotMachine.valid || !canRoll || !timeEnded),
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
        ))

        if (slotMachine.content.size > SlotMachine.PAGE_CHUNK) {
            val totalPage = ceil(slotMachine.content.size * 1.0 / SlotMachine.PAGE_CHUNK).toInt()

            result.add(ActionRow.of(
                Button.secondary("prev", "Previous Rewards").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next Rewards").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage)
            ))
        }

        return result
    }
}