package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class CardPackContentHolder(
    author: Message,
    channelID: String,
    private val message: Message,
    private val pack: CardPack
) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "pair" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val cardChancePairList = pack.cardChancePairLists[index]

                connectTo(event, PackContentAdjustHolder(authorMessage, channelID, message, pack, cardChancePairList, false))
            }
            "add" -> {
                val cardChancePairList = CardChancePairList(0)

                connectTo(event, PackContentAdjustHolder(authorMessage, channelID, message, pack, cardChancePairList, true))
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
        }
    }

    override fun onBack() {
        super.onBack()

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult() {
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
        val builder = StringBuilder("## Pack Content Adjust Menu\nPack name : ")
            .append(pack.packName)
            .append("\n\n")

        if (pack.cardChancePairLists.isEmpty()) {
            builder.append("- This pack doesn't have any content")
        } else {
            pack.cardChancePairLists.forEachIndexed { index, cardChancePairList ->
                builder.append(index + 1).append(". ").append(cardChancePairList.amount).append(" card")

                if (cardChancePairList.amount > 1)
                    builder.append("s")

                builder.append("\n")

                cardChancePairList.pairs.forEachIndexed { i, pair ->
                    builder.append("  - ").append(CardData.df.format(pair.chance)).append("% : ").append(pair.cardGroup.getName())

                    if (i < cardChancePairList.pairs.size - 1)
                        builder.append("\n")
                }

                if (index < pack.cardChancePairLists.size - 1)
                    builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (pack.cardChancePairLists.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            for (i in pack.cardChancePairLists.indices) {
                options.add(SelectOption.of((i + 1).toString(), i.toString()))
            }

            result.add(ActionRow.of(
                StringSelectMenu.create("pair")
                    .addOptions(options)
                    .setPlaceholder("Select card/chance pair list to adjust it")
                    .build()
            ))
        }

        result.add(
            ActionRow.of(
                Button.secondary("add", "Add Card/Chance Pair List")
                    .withEmoji(Emoji.fromUnicode("➕"))
                    .withDisabled(pack.cardChancePairLists.size >= StringSelectMenu.OPTIONS_MAX_AMOUNT)
            )
        )

        result.add(ActionRow.of(Button.secondary("back", "Go Back")))

        return result
    }
}