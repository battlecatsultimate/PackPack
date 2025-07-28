package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import kotlin.math.min

class CardCheckHolder(author: Message, userID: String, channelID: String, message: Message, private val members: List<Member>, private val tier: CardData.Tier) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Check list expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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
            "close" -> {
                event.deferEdit()
                    .setContent("Closed")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getText())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getText() : String {
        val builder = StringBuilder()

        if (members.isNotEmpty()) {
            builder.append("Below list is members who have T${tier.ordinal} cards\n\n")

            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(members.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                val inventory = Inventory.getInventory(members[i].idLong)

                builder.append(i + 1)
                    .append(". ")
                    .append(members[i].asMention)
                    .append(" [")
                    .append(members[i].id)
                    .append("] x")
                    .append(
                        inventory.cards.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value } +
                                inventory.favorites.entries.filter { e -> e.key.tier == tier }.sumOf { e -> e.value }
                    )
                    .append("\n")
            }
        } else {
            builder.append("There are no members who have T${tier.ordinal} cards yet")
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val dataSize = members.size

        val totalPage = getTotalPage(dataSize)

        if (dataSize > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
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

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }
}