package mandarin.card.supporter.holder.slot

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineEmojiSearchModalHolder
import mandarin.card.supporter.slot.*
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.ceil
import kotlin.math.min

class SlotMachinePlaceHolderRewardHolder(
    author: Message,
    channelID: String,
    private val message: Message,
    private val slotMachine: SlotMachine,
    private val content: SlotPlaceHolderContent,
    private val new: Boolean
) : ComponentHolder(author, channelID, message) {
    private var emojiName = ""
    private var page = 0

    private val actualEmojis = SlotEmojiContainer.loadedEmoji.filter { e -> emojiName in e.name.lowercase() && !slotMachine.content.any { c -> c.emoji?.name == e.name && c.emoji?.idLong == e.idLong } }.toMutableList()

    override fun clean() {

    }

    override fun onExpire(id: String?) {

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

                connectTo(SlotMachineEmojiSearchModalHolder(authorMessage, channelID, message) {
                    emojiName = it.lowercase()

                    updateEmojiStatus()
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
                CardBot.saveCardData()

                goBack(event)
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this reward? This cannot be undone", LangID.EN)

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    slotMachine.content.remove(content)

                    e.deferReply().setContent("Successfully removed reward!").setEphemeral(true).queue()

                    goBack()
                }, LangID.EN))
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

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    fun updateEmojiStatus() {
        actualEmojis.clear()

        actualEmojis.addAll(SlotEmojiContainer.loadedEmoji.filter { e -> emojiName in e.name.lowercase() && !slotMachine.content.any { c -> c.emoji?.name == e.name && c.emoji?.idLong == e.idLong } })

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

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "# ${slotMachine.name}\n" +
                "## Slot Machine Reward Create Section\n" +
                "In this section, you can create reward of this slot machine." +
                " First, you will have to decide emoji." +
                " If you want to add new emoji for this slot machine, you will have to manually add emoji in registered slot machine emoji server" +
                " (Server invite link i in pinned message in <#${CardData.managerPlace}>)." +
                " As soon as you add new emoji, bot will update its message, so that you can select that emoji\n" +
                "\n" +
                "This reward has Place Holder reward type." +
                " This is only for adding emojis to slot machine." +
                " It doesn't perform anything more than this, so as long as you decided the emoji, it's all done\n" +
                "### Reward Info\n" +
                "- **Emoji** : ${content.emoji?.formatted ?: "None"}\n" +
                "- **Reward Type** : Place Holder"
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

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