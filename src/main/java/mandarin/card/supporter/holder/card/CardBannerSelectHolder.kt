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
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import kotlin.math.max
import kotlin.math.min

class CardBannerSelectHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0
        set(value) {
            field = value

            val totalPage = getTotalPage(CardData.banners.size)

            field = max(0, min(field, totalPage - 1))
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
            "none" -> {
                card.banner.clear()

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

                val banner = CardData.banners[index]

                if (banner in card.banner) {
                    card.banner.remove(banner)
                } else {
                    card.banner.add(banner)
                }

                applyResult(event)
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
        val builder = StringBuilder("Select the banner that you want to assign to\nAdditionally, click `None` button for no banner\n\n")

        if (CardData.banners.isEmpty()) {
            builder.append("- No banner")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val banner = CardData.banners[i]

                val switch = if (banner in card.banner) {
                    EmojiStore.SWITCHON.formatted
                } else {
                    EmojiStore.SWITCHOFF.formatted
                }

                builder.append(i + 1).append(". ").append(banner.name).append(" ").append(switch).append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(Button.secondary("none", "None").withEmoji(EmojiStore.CROSS).withDisabled(card.banner.isEmpty())))

        val options = ArrayList<SelectOption>()

        if (CardData.banners.isNotEmpty()) {
            for (i in page * SearchHolder.PAGE_CHUNK until min(CardData.banners.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val banner = CardData.banners[i]

                val switch = if (banner in card.banner) {
                    EmojiStore.SWITCHON
                } else {
                    EmojiStore.SWITCHOFF
                }

                options.add(SelectOption.of(banner.name, i.toString()).withEmoji(switch))
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

        result.add(ActionRow.of(Button.secondary("back", "Back").withEmoji(EmojiStore.BACK)))

        return result
    }
}