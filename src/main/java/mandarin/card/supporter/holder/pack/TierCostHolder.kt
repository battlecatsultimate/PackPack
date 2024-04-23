package mandarin.card.supporter.holder.pack

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.TierCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class TierCostHolder(author: Message, channelID: String, private val message: Message, private val pack: CardPack, private val cardCost: TierCardCost, private val new: Boolean) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                cardCost.tier = CardPack.CardType.valueOf(event.values[0])

                if (pack in CardData.cardPacks) {
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

                connectTo(CardCostAmountHolder(authorMessage, channelID, message.id, pack, cardCost))
            }
            "create" -> {
                pack.cost.cardsCosts.add(cardCost)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card cost to this card pack!")
                    .setEphemeral(true)
                    .queue()

                expired = true

                parent?.goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone",
                        LangID.EN
                    )

                    StaticStore.removeHolder(authorMessage.author.id, this)

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                        expired = true

                        e.deferEdit().queue()

                        parent?.goBack()

                        return@ConfirmPopUpHolder null
                    }, { e ->
                        StaticStore.putHolder(authorMessage.author.id, this)

                        applyResult(e)

                        return@ConfirmPopUpHolder null
                    }, LangID.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card cost? This can't be undone",
                    LangID.EN
                )

                StaticStore.removeHolder(authorMessage.author.id, this)

                StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                    expired = true

                    pack.cost.cardsCosts.remove(cardCost)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
                        .setEphemeral(true)
                        .queue()

                    goBack()

                    return@ConfirmPopUpHolder null
                }, { e ->
                    StaticStore.putHolder(authorMessage.author.id, this)

                    applyResult(e)

                    return@ConfirmPopUpHolder null
                }, LangID.EN))
            }
        }
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack() {
        super.onBack()

        applyResult()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

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

            options.add(SelectOption.of(tierName, tier.name))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("tier")
                    .addOptions(options)
                    .setPlaceholder("Select card type to enable/disable as cost")
                    .build()
            )
        )

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
                    Button.secondary("back", "Go Back").withDisabled(cardCost.isInvalid()),
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

            val checkSymbol = if (tier == cardCost.tier) {
                EmojiStore.SWITCHON.formatted
            } else {
                EmojiStore.SWITCHOFF.formatted
            }

            builder.append(tierName).append(" : ").append(checkSymbol).append("\n")
        }

        builder.append("\nSelect tier to disable/enable")

        if (cardCost.isInvalid()) {
            builder.append("\n\n**Warning : This card cost is invalid because it requires 0 card, or it doesn't have any tier required**")
        }

        return builder.toString()
    }
}