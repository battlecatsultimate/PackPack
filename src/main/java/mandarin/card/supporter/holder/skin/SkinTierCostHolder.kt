package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.TierCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
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

class SkinTierCostHolder(author: Message, channelID: String, message: Message, private val skin: Skin, private val cardCost: TierCardCost, private val new: Boolean) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                cardCost.tier = CardPack.CardType.valueOf(event.values[0])

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

                connectTo(CardCostAmountHolder(authorMessage, channelID, message, cardCost))
            }
            "create" -> {
                skin.cost.cardsCosts.add(cardCost)

                if (skin in CardData.skins) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card cost to this card pack!")
                    .setEphemeral(true)
                    .queue()

                goBackTo(SkinCostManageHolder::class.java)
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone"
                    )

                    StaticStore.removeHolder(authorMessage.author.id, this)

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        goBackTo(e, SkinCostManageHolder::class.java)
                    }, { e ->
                        StaticStore.putHolder(authorMessage.author.id, this)

                        applyResult(e)
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

                StaticStore.removeHolder(authorMessage.author.id, this)

                StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    skin.cost.cardsCosts.remove(cardCost)

                    if (skin in CardData.skins) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, { e ->
                    StaticStore.putHolder(authorMessage.author.id, this)

                    applyResult(e)
                }, CommonStatic.Lang.Locale.EN))
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