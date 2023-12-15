package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.math.ceil
import kotlin.math.min

class RankListHolder(author: Message, channelID: String, messageID: String, private val users: List<String>, private val currencies: List<Long>, private val catFood: Boolean) : ComponentHolder(author, channelID, messageID) {
    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "prev" -> {
                page--

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
                CardBot.saveCardData()

                event.deferEdit()
                    .setContent("Closed the list")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
        }
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getRankList())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getRankList() : String {
        val rank = users.indexOf(authorMessage.author.id)

        val builder = StringBuilder(
            if (rank != -1) {
                "Your ranking is #${rank + 1}"
            } else {
                "You aren't listed in ranking list"
            }
        ).append("\n\n")

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, users.size)

        for (m in page * SearchHolder.PAGE_CHUNK until size) {
            builder.append(m + 1).append(". <@").append(users[m]).append("> : ").append(EmojiStore.ABILITY[if (catFood) "CF" else "SHARD"]?.formatted).append(" ").append(currencies[m])

            if (m < size - 1)
                builder.append("\n")
        }

        if (users.size > SearchHolder.PAGE_CHUNK) {
            val totalPage = ceil(users.size / SearchHolder.PAGE_CHUNK * 1.0).toInt()

            builder.append("\n\n").append("Page : ").append(page + 1).append("/").append(totalPage)
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val totalPage = getTotalPage(users.size)

        if (users.size > SearchHolder.PAGE_CHUNK) {
            val pages = ArrayList<ActionComponent>()

            if (users.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            pages.add(Button.of(ButtonStyle.SECONDARY,"prev", "Previous Page", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))
            pages.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (users.size > SearchHolder.PAGE_CHUNK * 10) {
                pages.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(pages))
        }

        result.add(ActionRow.of(Button.danger("close", "Close")))

        return result
    }
}