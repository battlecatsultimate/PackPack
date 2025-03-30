package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardChanceHolder
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardPack
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
import kotlin.math.min

class CardChancePairHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val pack: CardPack,
    private val cardChancePairList: CardChancePairList,
    private val pair: CardChancePair,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "chance" -> {
                val input = TextInput.create("chance", "Chance", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide chance (Percentage)")
                    .build()

                val modal = Modal.create("chance", "Card Chance Adjustment")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardChanceHolder(authorMessage, userID, channelID, message, pack, pair))
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedTier = CardPack.CardType.valueOf(event.values[0])

                if (selectedTier in pair.cardGroup.types) {
                    pair.cardGroup.types.remove(selectedTier)
                } else {
                    pair.cardGroup.types.add(selectedTier)
                }

                pair.cardGroup.types.sortBy { t -> t.ordinal }

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedBanner = CardData.banners[event.values.first().toInt()]

                if (selectedBanner in pair.cardGroup.extra) {
                    pair.cardGroup.extra.remove(selectedBanner)
                } else {
                    pair.cardGroup.extra.add(selectedBanner)
                }

                pair.cardGroup.extra.sortBy { b -> b.name }

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "create" -> {
                cardChancePairList.pairs.add(pair)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully create card/chance pair! Check result above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card/chance pair? This cannot be undone"
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBack(e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    goBack(event)
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete this card/chance pair? This cannot be undone"
                )

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    cardChancePairList.pairs.remove(pair)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card/chance pair!")
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

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder("## Card/Chance Pair Adjust Menu\n\nPack name : ")
            .append(pack.packName)
            .append("\n\nChance : ")
            .append(CardData.df.format(pair.chance))
            .append("%\n\n### Tier\n")

        CardPack.CardType.entries.forEach { tier ->
            val tierName = when(tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            val emoji = if (tier in pair.cardGroup.types)
                EmojiStore.SWITCHON
            else
                EmojiStore.SWITCHOFF

            builder.append("- ")
                .append(tierName)
                .append(" : ")
                .append(emoji.formatted)
                .append("\n")
        }

        builder.append("### Banner\n")

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.banners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = CardData.banners[i].name

            val emoji = if (CardData.banners[i] in pair.cardGroup.extra) {
                EmojiStore.SWITCHON.formatted
            } else {
                EmojiStore.SWITCHOFF.formatted
            }

            builder.append(bannerName).append(" : ").append(emoji).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("chance", "Adjust Chance")))

        val tierOptions = ArrayList<SelectOption>()

        CardPack.CardType.entries.forEach { tier ->
            val tierName = when (tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            tierOptions.add(SelectOption.of(tierName, tier.name))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("tier")
                    .addOptions(tierOptions)
                    .setPlaceholder("Select card type to enable/disable")
                    .build()
            )
        )

        val bannerOptions = ArrayList<SelectOption>()

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, CardData.banners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = CardData.banners[i].name

            bannerOptions.add(SelectOption.of(bannerName, i.toString()))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("banner")
                    .addOptions(bannerOptions)
                    .setPlaceholder("Select banner to enable/disable")
                    .build()
            )
        )

        val buttons = ArrayList<Button>()

        val totalPage = Holder.getTotalPage(CardData.banners.size)

        if (totalPage > 10) {
            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
        }

        buttons.add(Button.secondary("prev", "Previous Page").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

        buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

        if (totalPage > 10) {
            buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
        }

        result.add(ActionRow.of(buttons))

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create"),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back"),
                    Button.danger("delete", "Delete")
                )
            )
        }

        return result
    }
}