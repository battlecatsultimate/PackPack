package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import kotlin.math.min

class ActivatorHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val banners = CardData.banners

    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Activator expired")
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
            "confirm" -> {
                event.deferEdit()
                    .setContent("Confirmed activation of banners")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values.first().toInt()

                if (banners[index] in CardData.activatedBanners) {
                    CardData.activatedBanners.remove(banners[index])
                } else {
                    CardData.activatedBanners.add(banners[index])
                }

                applyResult(event)
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getText())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val banners = CardData.banners

        val dataSize = banners.size

        val bannerOptions = ArrayList<SelectOption>()

        for (i in SearchHolder.PAGE_CHUNK * page until min(dataSize, SearchHolder.PAGE_CHUNK * (page + 1))) {
            bannerOptions.add(SelectOption.of(banners[i].name, i.toString()).withEmoji(if (banners[i] in CardData.activatedBanners) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF))
        }

        rows.add(ActionRow.of(
            StringSelectMenu.create("banner").addOptions(bannerOptions).setPlaceholder("Select banner to activate/deactivate").build()
        ))

        val totalPage = getTotalPage(dataSize)

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("confirm", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder("Select banners to activate/deactivate\n\n")

        val banners = CardData.banners

        for (i in SearchHolder.PAGE_CHUNK * page until min(SearchHolder.PAGE_CHUNK * (page + 1), CardData.banners.size)) {
            builder.append("**")
                .append(banners[i].name)
                .append("** : ")

            if (banners[i] in CardData.activatedBanners) {
                builder.append("Activated")
            } else {
                builder.append("Deactivated")
            }

            builder.append("\n")
        }

        return builder.toString()
    }
}