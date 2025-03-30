package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.holder.pack.CardPackCostHolder
import mandarin.card.supporter.pack.BannerCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
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
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.min

class SkinBannerCostHolder(author: Message, userID: String, channelID: String, message: Message, private val skin: Skin, private val cardCost: BannerCardCost, private val new: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {

    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Skin manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val banner = CardData.banners[event.values.first().toInt()]

                cardCost.banner = banner

                if (skin in CardData.skins) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of required cards")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("amount", "Required Cards Amount")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardCostAmountHolder(authorMessage, userID, channelID, message, cardCost))
            }
            "create" -> {
                skin.cost.cardsCosts.add(cardCost)

                if (skin in CardData.skins) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card cost to this skin!")
                    .setEphemeral(true)
                    .queue()

                goBackTo(CardPackCostHolder::class.java)
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone"
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBackTo(e, CardPackCostHolder::class.java)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    if (skin in CardData.skins) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card cost? This can't be undone"
                )

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    skin.cost.cardsCosts.remove(cardCost)

                    if (skin in CardData.skins) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
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
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.banners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = CardData.banners[i].name

            options.add(SelectOption.of(bannerName, i.toString()))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("banner")
                    .addOptions(options)
                    .setPlaceholder("Select card type to enable/disable as cost")
                    .build()
            )
        )

        val totalPage = getTotalPage(CardData.banners.size)

        val buttons = ArrayList<Button>()

        if (totalPage > 10) {
            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
        }

        buttons.add(Button.secondary("prev", "Previous Page").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

        buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

        if (totalPage > 10) {
            buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
        }

        result.add(ActionRow.of(buttons))

        result.add(ActionRow.of(Button.secondary("amount", "Set Amount of Cards")))

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create").withDisabled(cardCost.isInvalid()),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back"),
                    Button.danger("delete", "Delete Cost")
                )
            )
        }

        return result
    }

    private fun getContents() : String {
        val builder = StringBuilder("Required amount : ")
            .append(cardCost.amount)
            .append("\n\n")

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.banners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = CardData.banners[i].name

            val checkSymbol = if (CardData.banners[i] === cardCost.banner) {
                EmojiStore.SWITCHON.formatted
            } else {
                EmojiStore.SWITCHOFF.formatted
            }

            builder.append(bannerName).append(" : ").append(checkSymbol).append("\n")
        }

        builder.append("\nSelect banner to disable/enable")

        if (cardCost.isInvalid()) {
            builder.append("\n\n**Warning : This card cost is invalid because it requires 0 card, or it doesn't have any banner required**")
        }

        return builder.toString()
    }
}