package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SlotMachinePackImportHolder(author: Message, userID: String, channelID: String, message: Message, private val slotMachine: SlotMachine, private val content: SlotCardContent) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val pack = CardData.cardPacks[event.values[0].toInt()]

                if (content.cardChancePairLists.isNotEmpty()) {
                    registerPopUp(event, "Are you sure you want to import this card pack? Slot machine contents will be overwritten and this cannot be undone")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        content.injectCardPack(pack)

                        e.deferReply().setContent("Successfully imported card pack : ${pack.packName}!").setEphemeral(true).queue()

                        goBack()
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    content.injectCardPack(pack)

                    event.deferReply().setContent("Successfully imported card pack : ${pack.packName}!").setEphemeral(true).queue()

                    goBack()
                }
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

    private fun getContents() : String {
        val builder = StringBuilder()

        builder.append("# ").append(slotMachine.name).append("\n")
            .append("## Slot Machine Reward Create Section\n")
            .append("In this section, you can import specific card pack into this card reward.")
            .append(" This can be useful if you want to put whole card pack, this will reduce tediousness of creating card pack from scratch\n")
            .append("### List of Card Packs\n")

        for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(CardData.cardPacks.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
            builder.append(i + 1).append(". ").append(CardData.cardPacks[i].packName).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        if (CardData.cardPacks.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(CardData.cardPacks.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                val emoji = if (EmojiStore.PACK[CardData.cardPacks[i]]?.isNotEmpty() == true) {
                    EmojiStore.PACK[CardData.cardPacks[i]]?.random()
                } else {
                    null
                }

                options.add(SelectOption.of(CardData.cardPacks[i].packName, i.toString()).withEmoji(emoji))
            }
        }

        result.add(ActionRow.of(StringSelectMenu.create("pack").addOptions(options).setDisabled(CardData.cardPacks.isEmpty()).setPlaceholder("Select Card Pack To Be Imported").build()))

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