package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineCardRewardNameModalHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineContentSlotModalHolder
import mandarin.card.supporter.holder.modal.slot.SlotMachineEmojiSearchModalHolder
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotEmojiContainer
import mandarin.card.supporter.slot.SlotMachine
import mandarin.card.supporter.slot.SlotPlaceHolderContent
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
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineCardRewardHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val slotMachine: SlotMachine,
    private val content: SlotCardContent,
    private val new: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var emojiName = ""

    private val actualEmojis = SlotEmojiContainer.loadedEmoji.filter { e -> emojiName in e.name.lowercase() && !slotMachine.content.filter { c -> c !== content && c is SlotPlaceHolderContent }.any { c -> c.emoji?.name == e.name && c.emoji?.id == e.id } }.toMutableList()

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

                val modal = Modal.create("search", "Emoji Search").addActionRow(input).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineEmojiSearchModalHolder(authorMessage, userID, channelID, message) {
                    emojiName = it.lowercase()

                    updateEmojiStatus()
                })
            }
            "pairList" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val cardChancePairList = content.cardChancePairLists[event.values[0].toInt()]

                connectTo(event, SlotMachineCardChancePairListHolder(authorMessage, userID, channelID, message, slotMachine, content, cardChancePairList, false))
            }
            "slot" -> {
                val input = TextInput.create("size", "Size", TextInputStyle.SHORT).setRequired(true).setPlaceholder("Put Slot Size Here").build()

                val modal = Modal.create("slot", "Slot Content Required Slot Size").addActionRow(input).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineContentSlotModalHolder(authorMessage, userID, channelID, message, slotMachine, content))
            }
            "add" -> {
                connectTo(event, SlotMachineCardChancePairListHolder(authorMessage, userID, channelID, message, slotMachine, content, CardChancePairList(0), true))
            }
            "pack" -> {
                connectTo(event, SlotMachinePackImportHolder(authorMessage, userID, channelID, message, slotMachine, content))
            }
            "name" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT).setRequired(true).setPlaceholder("Decide Name Here").setRequiredRange(1, 50).build()

                val modal = Modal.create("name", "Card Reward Name").addActionRow(input).build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineCardRewardNameModalHolder(authorMessage, userID, channelID, message, content))
            }
            "create" -> {
                slotMachine.content.add(content)

                if (slotMachine !in CardData.slotMachines) {
                    CardBot.saveCardData()
                }

                event.deferReply().setContent("Successfully added reward to slot machine!").setEphemeral(true).queue()

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
        val builder = StringBuilder()

        builder.append("# ").append(slotMachine.name).append("\n")
            .append("## Slot Machine Reward Create Section\n")
            .append("In this section. you can create reward of this slot machine.")
            .append(" First, you will have to decide emoji.")
            .append(" If you want to add new emoji for this slot machine, you will have to manually add emoji in registered slot machine emoji server")
            .append(" (Server invite link is in pinned message in <#${CardData.managerPlace}>).")
            .append(" As soon as you add new emoji, bot will update its message, so that you can select that emoji\n\n")
            .append("This reward has Card reward type.")
            .append(" You can adjust RNG pool of this reward by clicking buttons below\n")
            .append("### Reward Info\n")
            .append("- **Emoji** : ")
            .append(content.emoji?.formatted ?: "None")
            .append("\n")
            .append("- **Required Slot** : ")
            .append(content.slot)
            .append("\n")
            .append("- **Chance** : ")
            .append(slotMachine.getOdd(content).round(MathContext(5, RoundingMode.HALF_EVEN)).toPlainString())
            .append(" %\n")
            .append("- **RewardType** : Card\n")
            .append("- **Name** : ")
            .append(content.name.ifBlank { "None" })
            .append("\n")
            .append("- **Card Pool** :\n")

        if (content.cardChancePairLists.isEmpty()) {
            builder.append("- None")
        } else {
            content.cardChancePairLists.forEachIndexed { index, it ->
                builder.append(index + 1).append(". ").append(it.amount).append(" ")

                if (it.amount >= 2) {
                    builder.append("Cards")
                } else {
                    builder.append("Card")
                }

                builder.append("\n")

                it.pairs.forEach { pair ->
                    builder.append("  - ").append(CardData.df.format(pair.chance)).append("% : ").append(pair.cardGroup.getName()).append("\n")
                }
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val emojiOption = ArrayList<SelectOption>()

        if (actualEmojis.isEmpty()) {
            emojiOption.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(actualEmojis.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val e = actualEmojis[i]

                emojiOption.add(SelectOption.of(e.name, i.toString()).withEmoji(e).withDescription(e.id).withDefault(e.name == content.emoji?.name && e.id == content.emoji?.id))
            }
        }

        result.add(ActionRow.of(StringSelectMenu.create("emoji").addOptions(emojiOption).setPlaceholder("Select Emoji").setDisabled(actualEmojis.isEmpty()).build()))

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

        val pairListOption = ArrayList<SelectOption>()

        val content = content

        if (content.cardChancePairLists.isEmpty()) {
            pairListOption.add(SelectOption.of("A", "A"))
        } else {
            content.cardChancePairLists.forEachIndexed { index, _ ->
                pairListOption.add(SelectOption.of((index + 1).toString(), index.toString()))
            }
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("pairList")
                .addOptions(pairListOption)
                .setPlaceholder("Select Card Chance Pair List")
                .setDisabled(content.cardChancePairLists.isEmpty())
                .build()
        ))

        val packEmoji = if (EmojiStore.PACK.isNotEmpty()) {
            EmojiStore.PACK.entries.filter { e -> e.value.isNotEmpty() }.random().value.random()
        } else {
            null
        }

        result.add(ActionRow.of(
            Button.secondary("slot", "Change Required Slot Size").withEmoji(Emoji.fromUnicode("🎰")),
            Button.secondary("add", "Create New Card Pair Chance List")
                .withEmoji(EmojiStore.CARDS.entries.filter { e -> e.value.isNotEmpty() }.random().value.random())
                .withDisabled(content.cardChancePairLists.size >= StringSelectMenu.OPTIONS_MAX_AMOUNT || content.emoji == null),
            Button.secondary("pack", "Import Card Pack").withEmoji(packEmoji).withDisabled(content.emoji == null),
            Button.secondary("name", "Change Name").withEmoji(Emoji.fromUnicode("🏷️")).withDisabled(content.emoji == null)
        ))

        val buttons = ArrayList<Button>()

        if (new) {
            buttons.add(Button.success("create", "Create").withEmoji(EmojiStore.CHECK).withDisabled(content.cardChancePairLists.isEmpty() || content.emoji == null || content.slot == 0))
            buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))
        } else {
            buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK).withDisabled(content.cardChancePairLists.isEmpty() || content.emoji == null || content.slot == 0))
            buttons.add(Button.danger("delete", "Delete").withEmoji(EmojiStore.CROSS))
        }

        if (slotMachine !in CardData.slotMachines) {
            buttons.add(Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS))
        }

        result.add(ActionRow.of(buttons))

        return result
    }
}