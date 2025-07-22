package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineAmountHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineContentSlotModalHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineEmojiSearchModalHolder
import mandarin.card.supporter.slot.*
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineCurrencyRewardHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val slotMachine: SlotMachine,
    private val content: SlotCurrencyContent,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    private var emojiName = ""
    private val actualEmojis = SlotEmojiContainer.loadedEmoji.filter { e -> emojiName in e.name.lowercase() && !slotMachine.content.filter { c -> c !== content && c is SlotPlaceHolderContent }.any { c -> c.emoji?.name == e.name && c.emoji?.id == e.id } }.toMutableList()

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
            "emoji" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                content.changeEmoji(actualEmojis[index])

                event.deferReply().setContent("Successfully set emoji to ${content.emoji?.formatted}!").setEphemeral(true).queue()

                applyResult()
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
            "search" -> {
                val input = TextInput.create("keyword", "Keyword", TextInputStyle.SHORT).setRequired(false).setPlaceholder("Empty Keyword For No Filter").build()

                val modal = Modal.create("search", "Emoji Search").addComponents(ActionRow.of(input)).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineEmojiSearchModalHolder(authorMessage, userID, channelID, message) {
                    emojiName = it.lowercase()

                    updateEmojiStatus()
                })
            }
            "slot" -> {
                val input = TextInput.create("size", "Size", TextInputStyle.SHORT).setRequired(true).setPlaceholder("Put Slot Size Here").build()

                val modal = Modal.create("slot", "Slot Content Required Slot Size").addComponents(ActionRow.of(input)).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineContentSlotModalHolder(authorMessage, userID, channelID, message, slotMachine, content))
            }
            "mode" -> {
                content.mode = SlotCurrencyContent.Mode.entries[(content.mode.ordinal + 1) % SlotCurrencyContent.Mode.entries.size]

                val modeName = when(content.mode) {
                    SlotCurrencyContent.Mode.FLAT -> "Flat"
                    SlotCurrencyContent.Mode.PERCENTAGE -> "Percentage"
                }

                event.deferReply().setContent("Changed mode to $modeName!").setEphemeral(true).queue()

                applyResult()
            }
            "amount" -> {
                val valueName = when(content.mode) {
                    SlotCurrencyContent.Mode.FLAT -> "Amount"
                    SlotCurrencyContent.Mode.PERCENTAGE -> "Percentage"
                }

                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT).setRequired(true).setPlaceholder("Decide $valueName Value").build()

                val modal = Modal.create("amount", "$valueName Value").addComponents(ActionRow.of(input)).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineAmountHolder(authorMessage, userID, channelID, message) { v ->
                    content.amount = v

                    applyResult()
                })
            }
            "create" -> {
                slotMachine.content.add(content)

                if (slotMachine in CardData.slotMachines)
                    CardBot.saveCardData()

                event.deferReply().setContent("Successfully added reward into slot machine content! Check the result above").setEphemeral(true).queue()

                goBackTo(SlotMachineContentHolder::class.java)
            }
            "back" -> {
                if (new) {
                    registerPopUp(event, "Are you sure you want to cancel creation of this reward? This cannot be undone")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBackTo(e, SlotMachineContentHolder::class.java)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    CardBot.saveCardData()

                    goBack(event)
                }
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this reward? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    slotMachine.content.remove(content)

                    e.deferReply().setContent("Successfully removed reward!").setEphemeral(true).queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
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

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    fun updateEmojiStatus() {
        actualEmojis.clear()

        actualEmojis.addAll(SlotEmojiContainer.loadedEmoji.filter { e -> emojiName in e.name.lowercase() && !slotMachine.content.filter { c -> c !== content && c is SlotPlaceHolderContent }.any { c -> c.emoji?.name == e.name && c.emoji?.id == e.id } })

        if (actualEmojis.isNotEmpty()) {
            val totalPage = ceil(actualEmojis.size * 1.0 / SearchHolder.PAGE_CHUNK).toInt()

            if (totalPage <= page) {
                page = totalPage - 1
            }
        }

        if (content.emoji != null && content.emoji !in SlotEmojiContainer.loadedEmoji) {
            content.changeEmoji(null)
        }

        applyResult()
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
        val modeName = when(content.mode) {
            SlotCurrencyContent.Mode.FLAT -> "Flat"
            SlotCurrencyContent.Mode.PERCENTAGE -> "Percentage"
        }

        val rewardEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]?.formatted
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]?.formatted
        }

        return "# ${slotMachine.name}\n" +
                "## Slot Machine Reward Create Section\n" +
                "In this section, you can create reward of this slot machine." +
                " First, you will have to decide emoji." +
                " If you want to add new emoji for this slot machine, you will have to manually add emoji in registered slot machine emoji server" +
                " (Server invite link i in pinned message in <#${CardData.managerPlace}>)." +
                " As soon as you add new emoji, bot will update its message, so that you can select that emoji\n" +
                "\n" +
                "This reward has Currency reward type." +
                " You will have to decide reward mode : Flat of Percentage." +
                " Flat mode will give fixed amount of currency to the users." +
                " Percentage mode will give specific percentage of what user had put into the slot machine." +
                " After this, you can decide the amount or the percentage value\n" +
                "### Reward Info\n" +
                "- **Emoji** : ${content.emoji?.formatted ?: "None"}\n" +
                "- **Required Slot** : ${content.slot}\n" +
                "- **Chance** : ${slotMachine.getOdd(content).round(MathContext(5, RoundingMode.HALF_EVEN)).toPlainString()} %\n" +
                "- **Reward Type** : Currency\n" +
                "- **Mode** : $modeName\n" +
                "- **Amount** : ${
                    when(content.mode) {
                        SlotCurrencyContent.Mode.FLAT -> "$rewardEmoji ${content.amount}"
                        SlotCurrencyContent.Mode.PERCENTAGE -> "${content.amount}% of Entry Fee"
                    }
                }"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        if (actualEmojis.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(actualEmojis.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val e = actualEmojis[i]

                options.add(SelectOption.of(e.name, i.toString()).withEmoji(e).withDescription(e.id).withDefault(content.emoji?.name == e.name && content.emoji?.id == e.id))
            }
        }

        result.add(ActionRow.of(StringSelectMenu.create("emoji").addOptions(options).setPlaceholder("Select Emoji").setDisabled(actualEmojis.isEmpty()).build()))

        if (actualEmojis.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(actualEmojis.size * 1.0 / SearchHolder.PAGE_CHUNK)

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.secondary("search", "Search Emoji").withEmoji(Emoji.fromUnicode("🔎")))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        } else {
            result.add(ActionRow.of(Button.secondary("search", "Search Emoji").withEmoji(Emoji.fromUnicode("🔎"))))
        }

        val modeName = when(content.mode) {
            SlotCurrencyContent.Mode.FLAT -> "Mode : Flat"
            SlotCurrencyContent.Mode.PERCENTAGE -> "Mode : Percentage"
        }

        val modeEmoji = when(content.mode) {
            SlotCurrencyContent.Mode.FLAT -> Emoji.fromUnicode("💵")
            SlotCurrencyContent.Mode.PERCENTAGE -> Emoji.fromUnicode("⚖️")
        }

        val amountEmoji = when(slotMachine.entryFee.entryType) {
            SlotEntryFee.EntryType.CAT_FOOD -> EmojiStore.ABILITY["CF"]
            SlotEntryFee.EntryType.PLATINUM_SHARDS -> EmojiStore.ABILITY["SHARD"]
        }

        result.add(
            ActionRow.of(
                Button.secondary("slot", "Change Required Slot Size").withEmoji(Emoji.fromUnicode("🎰")),
                Button.secondary("mode", modeName).withEmoji(modeEmoji),
                Button.secondary("amount", "Set Amount").withEmoji(amountEmoji)
            )
        )

        val buttons = ArrayList<Button>()

        if (new) {
            buttons.add(Button.success("create", "Create").withEmoji(EmojiStore.CHECK).withDisabled(content.emoji == null))
            buttons.add(Button.secondary("back", "Back").withEmoji(EmojiStore.BACK))
        } else {
            buttons.add(Button.secondary("back", "Back").withEmoji(EmojiStore.BACK))
            buttons.add(Button.danger("delete", "Delete").withEmoji(EmojiStore.CROSS))
        }

        if (slotMachine !in CardData.slotMachines) {
            buttons.add(Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS))
        }

        result.add(ActionRow.of(buttons))

        return result
    }
}