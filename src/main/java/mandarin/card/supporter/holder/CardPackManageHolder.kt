package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardPackNameHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
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

class CardPackManageHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

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

                expired = true
            }
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val pack = CardData.cardPacks[index]

                connectTo(event, CardPackAdjustHolder(authorMessage, channelID, message, pack, false))
            }
            "add" -> {
                val input = TextInput.create("name", "Name", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide name of card pack")
                    .build()

                val modal = Modal.create("name", "Card Pack Name")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardPackNameHolder(authorMessage, channelID, message, true, null))
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
                page -= 10

                applyResult(event)
            }
        }
    }

    override fun onBack() {
        super.onBack()

        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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
            val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.cardPacks.size)

            for (i in page * SearchHolder.PAGE_CHUNK until size) {
                builder.append(i + 1).append(". ").append(CardData.cardPacks[i].packName).append("\n")
            }

            if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
                val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

                builder.append("\nPage : 1/").append(totalPage)
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(
            ActionRow.of(
            Button.secondary("add", "Add Card Pack")
                .withEmoji(Emoji.fromUnicode("➕"))
        ))

        if (CardData.cardPacks.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.cardPacks.size)

            for (i in page * SearchHolder.PAGE_CHUNK until size) {
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

            if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
                val buttons = ArrayList<Button>()

                val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

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