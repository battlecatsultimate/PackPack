package mandarin.card.supporter.holder.banner

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.max
import kotlin.math.min

class BannerManageHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0
        set(value) {
            field = value

            val totalPage = getTotalPage(CardData.banners.size)

            field = max(0, min(field, totalPage - 1))
        }

    init {
        registerAutoExpiration(FIVE_MIN)
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
            "create" -> {
                var index = 0

                var name: String

                while(true) {
                    name = "Banner $index"

                    if (CardData.banners.any { b -> b.name == name })
                        index++
                    else
                        break
                }

                val banner = Banner(name, false)

                connectTo(event, BannerEditHolder(authorMessage, userID, channelID, message, banner, true))
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Banner manager closed")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values.first().toInt()

                val banner = CardData.banners[index]

                connectTo(event, BannerEditHolder(authorMessage, userID, channelID, message, banner, false))
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Banner manager expired")
            .setComponents()
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select the banner that you want to modify\nIf you want to create new one, click `Create New Banner`\n\n```md\n")

        if (CardData.banners.isEmpty()) {
            builder.append("No banner\n```")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". ").append(CardData.banners[i].name).append("\n")
            }

            builder.append("```")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        if (CardData.banners.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                options.add(SelectOption.of(CardData.banners[i].name, i.toString()))
            }

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("Select banner to assign").build()))
        } else {
            options.add(SelectOption.of("a", "a"))

            result.add(ActionRow.of(StringSelectMenu.create("banner").addOptions(options).setPlaceholder("No banner").setDisabled(true).build()))
        }

        val totalPage = getTotalPage(CardData.banners.size)

        if (CardData.banners.size > SearchHolder.PAGE_CHUNK) {
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
            Button.success("create", "Create New Banner").withEmoji(Emoji.fromUnicode("➕")),
            Button.secondary("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}