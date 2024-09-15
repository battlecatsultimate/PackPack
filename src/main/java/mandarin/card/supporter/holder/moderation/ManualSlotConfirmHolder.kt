package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.holder.modal.slot.SlotManualRollInputHolder
import mandarin.card.supporter.slot.SlotEntryFee
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.max

class ManualSlotConfirmHolder(author: Message, channelID: String, message: Message, private val member: Member, private val users: List<String>, private val slotMachine: SlotMachine) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    private var input = -1L

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when (event.componentId) {
            "roll" -> {
                val ch = event.messageChannel

                val entryFee = when(slotMachine.entryFee.entryType) {
                    SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                    SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
                }

                val realInput = if (slotMachine.entryFee.minimumFee == slotMachine.entryFee.maximumFee) {
                    if (slotMachine.entryFee.minimumFee == 0L) {
                        registerPopUp(event, "Are you sure you want to roll this slot Machine for ${users.size} member(s) with input of $entryFee${slotMachine.entryFee.minimumFee}?")

                        slotMachine.entryFee.minimumFee
                    } else {
                        registerPopUp(event, "Are you sure you want to roll this slot Machine for ${users.size} member(s)?")

                        0L
                    }
                } else {
                    registerPopUp(event, "Are you sure you want to roll this slot Machine for ${users.size} member(s) with input of $entryFee$input?")

                    input
                }

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferEdit()
                        .setContent("Rolling...! \uD83C\uDFB2")
                        .setComponents()
                        .queue { _ ->
                            try {
                                slotMachine.manualRoll(message, member, users, realInput)
                            } catch (_: Exception) {
                                Command.replyToMessageSafely(
                                    ch,
                                    "Bot failed to find provided user in this server",
                                    authorMessage
                                ) { a -> a }
                            }
                        }
                }, CommonStatic.Lang.Locale.EN))
            }
            "input" -> {
                val minimumInput = max(slotMachine.entryFee.minimumFee, 1)

                val input = TextInput.create("fee", "Entry Fee", TextInputStyle.SHORT).setPlaceholder("Put Entry Fee From $minimumInput To ${slotMachine.entryFee.maximumFee}").setRequired(true).build()

                val modal = Modal.create("roll", "Slot Machine Roll").addActionRow(input).build()

                event.replyModal(modal).queue()

                connectTo(SlotManualRollInputHolder(authorMessage, channelID, message, ::input, slotMachine))
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

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Manual slot machine roll expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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
        val entryEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        var result = slotMachine.asText(page) + "\n\nInput : ${if (input < 0L) "None" else "$entryEmoji${max(input, 0L)}"}"

        if (!slotMachine.valid) {
            result += "\n- This slot machine contains invalid condition! Please contact card managers"
        }

        if (slotMachine.entryFee.minimumFee != slotMachine.entryFee.maximumFee && input < 0L) {
            result += "\n- You have to define input first!"
        }

        return result
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("input", "Define Input").withEmoji(EmojiStore.ABILITY["CF"])
        ))

        result.add(ActionRow.of(
            Button.secondary("roll", "Roll!").withEmoji(Emoji.fromUnicode("🎰")).withDisabled(!slotMachine.valid || (slotMachine.entryFee.minimumFee != slotMachine.entryFee.maximumFee && input < 0L)),
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
        ))

        if (slotMachine.content.size > SlotMachine.PAGE_CHUNK) {
            val totalPage = getTotalPage(slotMachine.content.size)

            result.add(ActionRow.of(
                Button.secondary("prev", "Previous Rewards").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next Rewards").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage)
            ))
        }

        return result
    }
}