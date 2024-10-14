package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.*
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineContentHolder(author: Message, userID: String, channelID: String, message: Message, private val slotMachine: SlotMachine) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
                connectTo(event, SlotMachineRewardTypeHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "reward" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                when(val content = slotMachine.content[event.values[0].toInt()]) {
                    is SlotCurrencyContent -> connectTo(event, SlotMachineCurrencyRewardHolder(authorMessage, userID, channelID, message, slotMachine, content, false))
                    is SlotCardContent -> connectTo(event, SlotMachineCardRewardHolder(authorMessage, userID, channelID, message, slotMachine, content, false))
                    is SlotPlaceHolderContent -> connectTo(event, SlotMachinePlaceHolderRewardHolder(authorMessage, userID, channelID, message, slotMachine, content, false))
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
            "sort" -> {
                connectTo(event, SlotMachineContentSortHolder(authorMessage, userID, channelID, message, slotMachine))
            }
            "back" -> {
                goBack(event)
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult() {
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
        val builder = StringBuilder()
            .append("# ")
            .append(slotMachine.name)
            .append("\n" +
                    "## Slot Machine Reward Section\n" +
                    "In this section, you can manage rewards upon full sequence of same specific emoji." +
                    " Select reward to modify or remove it." +
                    " Click `Create New Reward` button to create new reward." +
                    " Reward type depends on entry fee type\n" +
                    "## List of Reward\n")

        if (slotMachine.content.isEmpty()) {
            builder.append("- No Rewards")
        } else {
            val emoji = when(slotMachine.entryFee.entryType) {
                SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
                SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
            }

            for (index in page * SlotMachine.PAGE_CHUNK until min(slotMachine.content.size, (page + 1) * SlotMachine.PAGE_CHUNK)) {
                val content = slotMachine.content[index]

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
                                    .append(" { Chance = ")
                                    .append(slotMachine.getOdd(content).round(MathContext(5, RoundingMode.HALF_EVEN)).toPlainString())
                                    .append("% }")
                            }
                            SlotCurrencyContent.Mode.PERCENTAGE -> {
                                builder.append(" [Percentage] : ")
                                    .append(content.amount)
                                    .append("% of Entry Fee")
                                    .append(" { Chance = ")
                                    .append(slotMachine.getOdd(content).round(MathContext(5, RoundingMode.HALF_EVEN)).toPlainString())
                                    .append("% }")
                            }
                        }
                    }
                    is SlotCardContent -> {
                        builder.append(" [Card] : ")
                            .append(content.name)
                            .append(" { Chance = ")
                            .append(slotMachine.getOdd(content).round(MathContext(5, RoundingMode.HALF_EVEN)).toPlainString())
                            .append("% }")
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

        val options = ArrayList<SelectOption>()

        if (slotMachine.content.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SlotMachine.PAGE_CHUNK until min(slotMachine.content.size, (page + 1) * SlotMachine.PAGE_CHUNK)) {
                val content = slotMachine.content[i]

                val description = when (content) {
                    is SlotCurrencyContent -> {
                        when (content.mode) {
                            SlotCurrencyContent.Mode.FLAT -> "[Flat] : ${content.amount}"
                            SlotCurrencyContent.Mode.PERCENTAGE -> "[Percentage] : ${content.amount}% of Entry Fee"
                        }
                    }
                    is SlotCardContent -> {
                        "[Card] : ${content.name}"
                    }
                    is SlotPlaceHolderContent -> {
                        "[Place Holder]"
                    }
                    else -> {
                        throw IllegalStateException("E/SlotMachineContentHolder::getComponents - Unknown slot machine content type : ${content.javaClass.name}")
                    }
                }

                options.add(SelectOption.of("${i + 1}. ${content.emoji?.name ?: "UNKNOWN"}", i.toString()).withEmoji(content.emoji).withDescription(description))
            }
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("reward")
                .addOptions(options)
                .setPlaceholder("Select Reward To Modify")
                .setDisabled(slotMachine.content.isEmpty())
                .build()
        ))

        if (slotMachine.content.size > SlotMachine.PAGE_CHUNK) {
            val totalPage = ceil(slotMachine.content.size * 1.0 / SlotMachine.PAGE_CHUNK).toInt()

            result.add(ActionRow.of(
                Button.secondary("prev", "Previous Rewards").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next Rewards").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage)
            ))
        }

        val emojiList = arrayOf("🍌", "🍇", "🥝", "🍊", "🍎")

        result.add(ActionRow.of(
            Button.secondary("create", "Create New Reward").withEmoji(Emoji.fromUnicode(emojiList.random())).withDisabled(slotMachine.content.size >= StringSelectMenu.OPTIONS_MAX_AMOUNT),
            Button.secondary("sort", "Sort Reward").withEmoji(Emoji.fromUnicode("🔧")).withDisabled(slotMachine.content.isEmpty())
        ))

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