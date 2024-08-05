package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineContentSortModalHolder
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotCurrencyContent
import mandarin.card.supporter.slot.SlotEntryFee
import mandarin.card.supporter.slot.SlotMachine
import mandarin.card.supporter.slot.SlotPlaceHolderContent
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.jcodec.api.UnsupportedFormatException
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineContentSortHolder(author: Message, channelID: String, message: Message, private val slotMachine: SlotMachine) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "back" -> {
                CardBot.saveCardData()

                goBack(event)
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, CommonStatic.Lang.Locale.EN))
            }
            else -> {
                when {
                    event.componentId.matches(Regex("up\\d+")) -> {
                        val index = event.componentId.replace("up", "").toInt()

                        if (index - 1 < 0)
                            return

                        val from = slotMachine.content[index]
                        val to = slotMachine.content[index - 1]

                        slotMachine.content[index - 1] = from
                        slotMachine.content[index] = to

                        applyResult(event)
                    }
                    event.componentId.matches(Regex("down\\d+")) -> {
                        val index = event.componentId.replace("down", "").toInt()

                        if (index + 1 >= slotMachine.content.size)
                            return

                        val from = slotMachine.content[index]
                        val to = slotMachine.content[index + 1]

                        slotMachine.content[index + 1] = from
                        slotMachine.content[index] = to

                        applyResult(event)
                    }
                    event.componentId.matches(Regex("set\\d+")) -> {
                        val index = event.componentId.replace("set", "").toInt()

                        if (index < 0 || index >= slotMachine.content.size)
                            return

                        val input = TextInput.create("index", "Place", TextInputStyle.SHORT).setRequired(true).setPlaceholder("Decide place to be set here").build()

                        val modal = Modal.create("sort", "Content Place Setter").addActionRow(input).build()

                        event.replyModal(modal).queue()

                        connectTo(SlotMachineContentSortModalHolder(authorMessage, channelID, message) { i, e ->
                            val targetIndex = i - 1

                            if (targetIndex < 0 || targetIndex >= slotMachine.content.size) {
                                e.deferReply()
                                    .setContent("Out of bounds! You have to offer only value in range from 1 to ${slotMachine.content.size}")
                                    .setEphemeral(true)
                                    .queue()

                                return@SlotMachineContentSortModalHolder
                            }

                            if (targetIndex == index) {
                                e.deferReply()
                                    .setContent("It's redundant to move content to same place")
                                    .setEphemeral(true)
                                    .queue()

                                return@SlotMachineContentSortModalHolder
                            }

                            val from = slotMachine.content[index]

                            if (targetIndex > index) {
                                slotMachine.content.add(targetIndex + 1, from)
                                slotMachine.content.removeAt(index)
                            } else {
                                slotMachine.content.add(targetIndex, from)
                                slotMachine.content.removeAt(index + 1)
                            }

                            applyResult(e)
                        })
                    }
                }
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
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

    private fun applyResult(event: ModalInteractionEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder()
            .append("# ")
            .append(slotMachine.name)
            .append("\n" +
                    "## Slot Machine Reward Sort Section\n" +
                    "In this section, you can sort rewards." +
                    " You can up button to move that reward upper, and down button to lower it." +
                    " Also by clicking `Set` button, you can move reward to exact place\n" +
                    "## List of Reward\n")

        if (slotMachine.content.isEmpty()) {
            builder.append("- No Rewards")
        } else {
            val emoji = when(slotMachine.entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            slotMachine.content.forEachIndexed { index, content ->
                builder.append(index + 1).append(". ").append(content.emoji?.formatted ?: EmojiStore.UNKNOWN.formatted)

                if (content !is SlotPlaceHolderContent) {
                    builder.append("x").append(content.slot)
                }

                when (content) {
                    is SlotCurrencyContent -> {
                        when(content.mode) {
                            SlotCurrencyContent.Mode.FLAT -> {

                                builder.append(" [Flat] : ")
                                    .append(emoji)
                                    .append(" ")
                                    .append(content.amount)
                            }
                            SlotCurrencyContent.Mode.PERCENTAGE -> {
                                builder.append(" [Percentage] : ")
                                    .append(content.amount)
                                    .append("% of Entry Fee")
                            }
                        }
                    }
                    is SlotCardContent -> {
                        builder.append(" [Card] : ")
                            .append(content.name)
                            .append("\n")

                        content.cardChancePairLists.forEachIndexed { ind, l ->
                            builder.append("  - ").append(l.amount).append(" ")

                            if (l.amount >= 2) {
                                builder.append("Cards\n")
                            } else {
                                builder.append("Card\n")
                            }

                            l.pairs.forEachIndexed { i, p ->
                                builder.append("    - ").append(CardData.df.format(p.chance)).append("% : ").append(p.cardGroup.getName())

                                if (i < l.pairs.size - 1)
                                    builder.append("\n")
                            }

                            if (ind < content.cardChancePairLists.size - 1)
                                builder.append("\n")
                        }
                    }
                    is SlotPlaceHolderContent -> {
                        builder.append(" [Place Holder]")
                    }
                }

                if (index < slotMachine.content.size - 1)
                    builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val feeEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        for (i in page * 3 until min(slotMachine.content.size, (page + 1) * 3)) {
            val content = slotMachine.content[i]

            val name = when(content) {
                is SlotCurrencyContent -> {
                    when(content.mode) {
                        SlotCurrencyContent.Mode.FLAT -> "[Flat] : $feeEmoji ${content.amount}"
                        SlotCurrencyContent.Mode.PERCENTAGE -> "[Percentage] : ${content.amount}% of Entry Fee"
                    }
                }
                is SlotCardContent -> {
                    "[Card] : ${content.name}"
                }
                is SlotPlaceHolderContent -> {
                    "[Placeholder]"
                }
                else -> {
                    throw UnsupportedFormatException("E/SlotMachineContentSortHolder::getComponents - Unknown content type found : ${content::class.java}")
                }
            }

            result.add(ActionRow.of(
                Button.secondary("reward$i", name).withEmoji(content.emoji),
                Button.secondary("up$i", "🔼").withDisabled(i == 0),
                Button.secondary("set$i", "Set"),
                Button.secondary("down$i", "🔽").withDisabled(i == slotMachine.content.size - 1)
            ))
        }

        if (slotMachine.content.size > 3) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(slotMachine.content.size * 1.0 / 3)

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

        if (slotMachine !in CardData.slotMachines) {
            result.add(ActionRow.of(
                Button.secondary("back", "Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Back").withEmoji(EmojiStore.BACK)
            ))
        }

        return result
    }
}