package mandarin.card.supporter.holder.card

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class CardBannerSelectHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "none" -> {
                card.banner = ""

                event.deferReply()
                    .setContent("Successfully set the banner to none!")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "banner" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                val index = event.values.first().toInt()

                card.banner = CardData.banners[index]

                event.deferReply()
                    .setContent("Successfully set the banner to `${card.banner}`!")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                goBack(event)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {

    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setFiles()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select the banner that you want to assign to\nAdditionally, click `None` button for no banner\n\n```md\n")

        if (CardData.banners.isEmpty()) {
            builder.append("No banner\n```")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". ").append(CardData.banners[i]).append("\n")
            }

            builder.append("```")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("none", "None").withEmoji(EmojiStore.CROSS).withDisabled(card.banner.isBlank())))

        val options = ArrayList<SelectOption>()

        if (CardData.banners.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                options.add(SelectOption.of(CardData.banners[i], i.toString()).withDefault(card.banner == CardData.banners[i]))
            }

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("Select banner to assign").build()))
        } else {
            options.add(SelectOption.of("a", "a"))

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("No banner").setDisabled(true).build()))
        }

        result.add(ActionRow.of(Button.secondary("back", "Back").withEmoji(EmojiStore.BACK)))

        return result
    }
}