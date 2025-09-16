package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardPackNameHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class CardPackManageHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "confirm" -> {
                event.deferEdit()
                    .setContent("Confirmed management!")
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .setComponents()
                    .queue()

                CardBot.saveCardData()

                end(true)
            }
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val pack = CardData.cardPacks[index]

                connectTo(event, CardPackAdjustHolder(authorMessage, userID, channelID, message, pack, false))
            }
            "add" -> {
                val input = TextInput.create("name", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide name of card pack")
                    .build()

                val modal = Modal.create("name", "Card Pack Name")
                    .addComponents(Label.of("Name", input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardPackNameHolder(authorMessage, userID, channelID, message, true, null))
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
        }
    }

    override fun onBack(child: Holder) {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder("Click `Add Card Pack` button to add new pack\nSelect pack to remove or modify the pack\n## List of card packs\n")

        if (CardData.cardPacks.isEmpty()) {
            builder.append("- No packs")
        } else {
            val size = min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, CardData.cardPacks.size)

            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until size) {
                val emoji = EmojiStore.getPackEmoji(CardData.cardPacks[i])

                val formatted = emoji?.formatted ?: ""

                builder.append(i + 1).append(". ").append(formatted).append(" ").append(CardData.cardPacks[i].packName).append("\n")
            }

            if (CardData.cardPacks.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                val totalPage = ceil(CardData.cardPacks.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize)

                builder.append("\nPage : 1/").append(totalPage)
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(
            ActionRow.of(
            Button.secondary("add", "Add Card Pack")
                .withEmoji(Emoji.fromUnicode("➕"))
        ))

        if (CardData.cardPacks.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            val size = min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, CardData.cardPacks.size)

            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until size) {
                val packName = if (CardData.cardPacks[i].packName.length >= 100) {
                    CardData.cardPacks[i].packName.substring(0, 50) + "..."
                } else {
                    CardData.cardPacks[i].packName
                }

                options.add(SelectOption.of(packName, i.toString()))
            }

            result.add(
                ActionRow.of(
                StringSelectMenu.create("pack")
                    .addOptions(options)
                    .setPlaceholder("Select Pack To Modify")
                    .build()
            ))

            if (CardData.cardPacks.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
                val buttons = ArrayList<Button>()

                val totalPage = ceil(CardData.cardPacks.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize)

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
        }

        result.add(
            ActionRow.of(
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}