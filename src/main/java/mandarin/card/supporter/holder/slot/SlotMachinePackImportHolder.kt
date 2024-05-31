package mandarin.card.supporter.holder.slot

import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class SlotMachinePackImportHolder(author: Message, channelID: String, private val message: Message, private val slotMachine: SlotMachine, private val content: SlotCardContent) : ComponentHolder(author, channelID, message) {
    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "pack" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val pack = CardData.cardPacks[event.values[0].toInt()]

                if (content.cardChancePairLists.isNotEmpty()) {
                    registerPopUp(event, "Are you sure you want to import this card pack? Slot machine contents will be overwritten and this cannot be undone", LangID.EN)

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        content.injectCardPack(pack)

                        e.deferReply().setContent("Successfully imported card pack : ${pack.packName}!").setEphemeral(true).queue()

                        goBack()
                    }, LangID.EN))
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

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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

        for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.cardPacks.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
            builder.append(i + 1).append(". ").append(CardData.cardPacks[i].packName).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        if (CardData.cardPacks.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.cardPacks.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val emoji = if (EmojiStore.PACK[CardData.cardPacks[i]]?.isNotEmpty() == true) {
                    EmojiStore.PACK[CardData.cardPacks[i]]?.random()
                } else {
                    null
                }

                options.add(SelectOption.of(CardData.cardPacks[i].packName, i.toString()).withEmoji(emoji))
            }
        }

        result.add(ActionRow.of(StringSelectMenu.create("pack").addOptions(options).setDisabled(CardData.cardPacks.isEmpty()).setPlaceholder("Select Card Pack To Be Imported").build()))

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