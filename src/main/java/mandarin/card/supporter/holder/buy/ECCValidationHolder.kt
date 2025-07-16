package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.ECCValidation
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.log.TransactionLogger
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import kotlin.collections.set

class ECCValidationHolder(author: Message, userID: String, channelID: String, message: Message, private val validationWay: ECCValidation.ValidationWay) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private val seasonalCards = ArrayList<Card>()
    private val collaborationCards = ArrayList<Card>()
    private val t4Cards = ArrayList<Card>()

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val guild = event.jda.getGuildById(CardData.guild) ?: return

        when(event.componentId) {
            "seasonal" -> {
                val cards = inventory.cards.keys.filter { c -> c.cardType == Card.CardType.SEASONAL }.toList().sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, seasonalCards, 15))
            }
            "collaboration" -> {
                val cards = inventory.cards.keys.filter { c -> c.cardType == Card.CardType.COLLABORATION}.toList().sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, collaborationCards, 12))
            }
            "t4" -> {
                val cards = if (validationWay == ECCValidation.ValidationWay.SAME_T4_3) {
                    inventory.cards.keys.filter { c -> c.tier == CardData.Tier.LEGEND }.filter { c -> (inventory.cards[c] ?: 0) >= 3 }
                } else {
                    inventory.cards.keys.filter { c -> c.tier == CardData.Tier.LEGEND }
                }

                if (validationWay == ECCValidation.ValidationWay.T4_2) {
                    connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, t4Cards, 2))
                } else {
                    connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, t4Cards, 1))
                }
            }
            "obtain" -> {
                if (validationWay != ECCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
                    registerPopUp(event, "Are you sure you want to obtain ECC with these cards? This cannot be undone!")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        obtainECC(guild, e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    obtainECC(guild, event)
                }
            }
            "back" -> {
                if (seasonalCards.isNotEmpty() || collaborationCards.isNotEmpty() || t4Cards.isNotEmpty()) {
                    registerPopUp(event, "Are you sure you want to go back? Your purchase progress will be lost")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBack(e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    goBack(event)
                }
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel purchase? Your purchase progress will be lost")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferEdit()
                        .setContent("Purchase canceled")
                        .setComponents()
                        .setAllowedMentions(arrayListOf())
                        .mentionRepliedUser(false)
                        .queue()

                    end(true)
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Purchase expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val doable = ECCValidation.checkDoable(validationWay, inventory)

        if (doable.isNotBlank()) {
            return "You can't validate ECC with this way because of the reasons below! :\n\n$doable"
        }

        if (validationWay == ECCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
            return "Are you sure you want to obtain ECC with this way? **Keep in mind that your ECC will be lost if you lose <@&${CardData.Role.LEGEND.id}>**"
        }

        val builder = StringBuilder("Please check validation status below :\n\n")

        when(validationWay) {
            ECCValidation.ValidationWay.SEASONAL_15_COLLAB_12_T4 -> {
                val seasonal = if (seasonalCards.size < 15) {
                    "❌"
                } else {
                    "⭕"
                }

                val collaboration = if (collaborationCards.size < 12) {
                    "❌"
                } else {
                    "⭕"
                }

                val t4 = if (t4Cards.isEmpty()) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 15 Seasonal Cards : ").append(seasonal).append("\n")
                    .append("- Unique 12 Collaboration Cards : ").append(collaboration).append("\n")
                    .append("- 1 T4 Card : ").append(t4)
            }
            ECCValidation.ValidationWay.T4_2 -> {
                val t4 = if (t4Cards.size < 2) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- 2 Unique T4 Cards : ").append(t4)
            }
            ECCValidation.ValidationWay.SAME_T4_3 -> {
                val t4 = if (t4Cards.isEmpty()) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- 3 Same T4 Cards : ").append(t4)
            }
            else -> throw IllegalStateException("E/ECCValidationHolder::getContents - Unhandled validation way : $validationWay")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (ECCValidation.checkDoable(validationWay, inventory).isNotBlank()) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))

            return result
        }

        if (validationWay == ECCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
            result.add(ActionRow.of(
                Button.success("obtain", "Obtain ECC!").withEmoji(EmojiStore.CHECK),
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))

            return result
        }

        var needSeasonal = false
        var needCollaboration = false
        var obtainable: Boolean

        when(validationWay) {
            ECCValidation.ValidationWay.SEASONAL_15_COLLAB_12_T4 -> {
                needSeasonal = true
                needCollaboration = true

                obtainable = seasonalCards.size >= 15 && collaborationCards.size >= 12 && t4Cards.isNotEmpty()
            }
            ECCValidation.ValidationWay.T4_2 -> {
                obtainable = t4Cards.size >= 2
            }
            ECCValidation.ValidationWay.SAME_T4_3 -> {
                obtainable = t4Cards.isNotEmpty()
            }
            else -> throw IllegalStateException("E/ECCValidationHolder::getComponents - Unhandled validation way : $validationWay")
        }

        if (needSeasonal) {
            result.add(ActionRow.of(Button.secondary("seasonal", "Pay Seasonal Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL))))
        }

        if (needCollaboration) {
            result.add(ActionRow.of(Button.secondary("collaboration", "Pay Collaboration Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION))))
        }

        val label = if (validationWay == ECCValidation.ValidationWay.T4_2) {
            "Pay T4 Cards"
        } else {
            "Pay T4 Card"
        }

        result.add(ActionRow.of(Button.secondary("t4", label).withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T3))))

        result.add(ActionRow.of(
            Button.success("obtain", "Obtain ECC!").withDisabled(!obtainable),
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }

    private fun obtainECC(guild: Guild, event: IMessageEditCallback) {
        inventory.eccValidation.validationWay = validationWay

        inventory.eccValidation.cardList.addAll(seasonalCards)
        inventory.eccValidation.cardList.addAll(collaborationCards)

        if (validationWay == ECCValidation.ValidationWay.SAME_T4_3) {
            repeat(3) {
                inventory.eccValidation.cardList.addAll(t4Cards)
            }
        } else {
            inventory.eccValidation.cardList.addAll(t4Cards)
        }

        seasonalCards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - 1 }
        collaborationCards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - 1 }
        t4Cards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - (if (validationWay == ECCValidation.ValidationWay.SAME_T4_3) 3 else 1) }

        CardBot.saveCardData()

        TransactionLogger.logECCObtain(authorMessage.author.idLong, inventory.eccValidation)

        val role = guild.roles.find { r -> r.id == CardData.ecc }

        if (role == null) {
            event.deferEdit()
                .setContent("Obtain was successful, but role couldn't be given to user. Please contact card manager!")
                .setComponents()
                .setAllowedMentions(arrayListOf())
                .mentionRepliedUser(false)
                .queue()

            end(true)
        } else {
            guild.addRoleToMember(UserSnowflake.fromId(userID), role).queue()

            event.deferEdit()
                .setContent("ECC obtained successfully! Check your role list")
                .setComponents()
                .setAllowedMentions(arrayListOf())
                .mentionRepliedUser(false)
                .queue()

            end(true)
        }
    }
}