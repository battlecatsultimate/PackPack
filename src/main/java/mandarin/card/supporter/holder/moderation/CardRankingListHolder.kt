package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.math.min

class CardRankingListHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    companion object {
        const val CHUNK_SIZE = 15
    }

    private val totalCards: Long
    private val entries = ArrayList<Map.Entry<Long, Int>>()

    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)

        val ranking = HashMap<Long, Int>()

        var amount = 0L

        CardData.inventories.entries.forEach { (userID, inventory) ->
            if (!inventory.cards.containsKey(card) && !inventory.favorites.containsKey(card)) {
                return@forEach
            }

            ranking[userID] = (inventory.cards[card] ?: 0) + (inventory.favorites[card] ?: 0)
            amount += ((inventory.cards[card] ?: 0) + (inventory.favorites[card] ?: 0)).toLong()
        }

        entries.addAll(ranking.entries.sortedByDescending { e -> e.value })
        totalCards = amount
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
                goBack(event)
            }
            "close" -> {
                message.editMessage("Card ranking closed")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card ranking expired").queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Ranking of users who have ").append(card.simpleCardInfo()).append("\nThere are in total ").append(totalCards).append(" cards").append("\n\n")

        if (entries.isEmpty()) {
            builder.append("- No one owns this card yet")
        } else {
            for (i in page * CHUNK_SIZE until min(entries.size, (page + 1) * CHUNK_SIZE)) {
                builder.append(i + 1).append(". <@").append(entries[i].key).append("> [").append(entries[i].key).append("] : x").append(entries[i].value).append("\n")
            }

            builder.append("\nPage : ").append(page + 1).append(" / ").append(getTotalPage(entries.size))
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val totalPage = getTotalPage(entries.size)

        if (entries.size > CHUNK_SIZE) {
            val buttons = ArrayList<Button>()

            if(totalPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS))
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS))
            }

            if(page + 1 >= totalPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))
            }

            if(totalPage > 10) {
                if(page + 10 >= totalPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.secondary("back", "Back").withEmoji(EmojiStore.BACK),
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}