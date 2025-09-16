package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.slot.SlotMachineCardChanceHolder
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SlotMachineCardChancePairHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val slotMachine: SlotMachine,
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
        message.editMessage("Slot machine manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "chance" -> {
                val input = TextInput.create("chance", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide chance (Percentage)")
                    .build()

                val modal = Modal.create("chance", "Card Chance Adjustment")
                    .addComponents(Label.of("Chance", input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(SlotMachineCardChanceHolder(authorMessage, userID, channelID, message, slotMachine, pair))
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

                if (slotMachine in CardData.slotMachines) {
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

                if (slotMachine in CardData.slotMachines) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "create" -> {
                cardChancePairList.pairs.add(pair)

                if (slotMachine in CardData.slotMachines) {
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
                    registerPopUp(event, "Are you sure you want to cancel creating card/chance pair? This cannot be undone")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBack(e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    if (slotMachine in CardData.slotMachines) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete this card/chance pair? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    cardChancePairList.pairs.remove(pair)

                    if (slotMachine in CardData.slotMachines) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card/chance pair!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBackTo(SlotMachineListHolder::class.java)
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

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
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
        val builder = StringBuilder()

        builder.append("# ").append(slotMachine.name).append("\n")
            .append("## Slot Machine Reward Create Section\n")
            .append("In this section, you can adjust card and chance pair.")
            .append(" You will have to decide chance value first.")
            .append(" Chance value's unit is percentage.")
            .append(" For example, if you want to set chance as 45.5%, you have to type `45.5`.")
            .append(" Next thing is deciding which pool you will activate when card/chance pair list picked this card/chance pair.")
            .append(" It allows pool per banner or tier\n")
            .append("### Card/Chance Pair Info\n")
            .append("- **Chance** : ").append(CardData.df.format(pair.chance)).append("%\n\n")
            .append("### Tier\n")

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

        val size = min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, CardData.banners.size)

        for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until size) {
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

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

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

        val size = min((page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize, CardData.banners.size)

        for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until size) {
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

        val totalPage = getTotalPage(CardData.banners.size)

        if (totalPage > 10) {
            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
        }

        buttons.add(Button.secondary("prev", "Previous Page").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

        buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

        if (totalPage > 10) {
            buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
        }

        result.add(ActionRow.of(buttons))

        val pageButtons = ArrayList<Button>()

        if (new) {
            pageButtons.add(Button.success("create", "Create").withEmoji(EmojiStore.CHECK))
            pageButtons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))
        } else {
            pageButtons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))
            pageButtons.add(Button.secondary("delete", "Delete").withEmoji(EmojiStore.CROSS))
        }

        if (slotMachine !in CardData.slotMachines) {
            pageButtons.add(Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS))
        }

        result.add(ActionRow.of(pageButtons))

        return result
    }
}