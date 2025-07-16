package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CCValidation
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

class CCValidationHolder(author: Message, userID: String, channelID: String, message: Message, private val validationWay: CCValidation.ValidationWay) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private val seasonalCards = ArrayList<Card>()
    private val collaborationCards = ArrayList<Card>()
    private val t3Cards = ArrayList<Card>()

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
            "t3" -> {
                val cards = inventory.cards.keys.filter { c -> c.tier == CardData.Tier.ULTRA }.toList().sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, t3Cards, 3))
            }
            "obtain" -> {
                if (validationWay != CCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
                    registerPopUp(event, "Are you sure you want to obtain CC with these cards? This cannot be undone!")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        obtainCC(guild, e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    obtainCC(guild, event)
                }
            }
            "back" -> {
                if (seasonalCards.isNotEmpty() || collaborationCards.isNotEmpty() || t3Cards.isNotEmpty()) {
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
        val doable = CCValidation.checkDoable(validationWay, inventory)

        if (doable.isNotBlank()) {
            return "You can't validate CC with this way because of the reasons below! :\n\n$doable"
        }

        if (validationWay == CCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
            return "Are you sure you want to obtain CC with this way? **Keep in mind that your CC will be lost if you lose <@&${CardData.Role.LEGEND.id}>**"
        }

        val builder = StringBuilder("Please check validation status below :\n\n")
        val cf = EmojiStore.ABILITY["CF"]?.formatted

        when(validationWay) {
            CCValidation.ValidationWay.SEASONAL_15 -> {
                val seasonal = if (seasonalCards.size < 15) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 15 Seasonal Cards : ").append(seasonal).append("\n").append("- $cf 150000 : ⭕")
            }
            CCValidation.ValidationWay.COLLABORATION_12 -> {
                val collaboration = if (collaborationCards.size < 12) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 12 Collaboration Cards : ").append(collaboration).append("\n").append("- $cf 150000 : ⭕")
            }
            CCValidation.ValidationWay.SEASONAL_15_COLLABORATION_12 -> {
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

                builder.append("- Unique 15 Seasonal Cards : ").append(seasonal).append("\n").append("- Unique 12 Collaboration Cards : ").append(collaboration)
            }
            CCValidation.ValidationWay.T3_3 -> {
                val t3 = if (t3Cards.size < 3) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 3 T3 Cards : ").append(t3).append("\n").append("- $cf 200000 : ⭕")
            }
            else -> throw IllegalStateException("E/CCValidationHolder::getContents - Unhandled validation way : $validationWay")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        if (CCValidation.checkDoable(validationWay, inventory).isNotBlank()) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))

            return result
        }

        if (validationWay == CCValidation.ValidationWay.LEGENDARY_COLLECTOR) {
            result.add(ActionRow.of(
                Button.success("obtain", "Obtain CC!").withEmoji(EmojiStore.CHECK),
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))

            return result
        }

        var needSeasonal = false
        var needCollaboration = false
        var needT3 = false
        var obtainable: Boolean

        when(validationWay) {
            CCValidation.ValidationWay.SEASONAL_15 -> {
                needSeasonal = true

                obtainable = seasonalCards.size >= 15
            }
            CCValidation.ValidationWay.COLLABORATION_12 -> {
                needCollaboration = true

                obtainable = collaborationCards.size >= 12
            }
            CCValidation.ValidationWay.SEASONAL_15_COLLABORATION_12 -> {
                needSeasonal = true
                needCollaboration = true

                obtainable = seasonalCards.size >= 15 && collaborationCards.size >= 12
            }
            CCValidation.ValidationWay.T3_3 -> {
                needT3 = true

                obtainable = t3Cards.size >= 3
            }
            else -> throw IllegalStateException("E/CCValidationHolder::getComponents - Unhandled validation way : $validationWay")
        }

        if (needSeasonal) {
            result.add(ActionRow.of(Button.secondary("seasonal", "Pay Seasonal Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL)).withDisabled(seasonalCards.size >= 15)))
        }

        if (needCollaboration) {
            result.add(ActionRow.of(Button.secondary("collaboration", "Pay Collaboration Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION)).withDisabled(collaborationCards.size >= 12)))
        }

        if (needT3) {
            result.add(ActionRow.of(Button.secondary("t3", "Pay T3 Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T3)).withDisabled(t3Cards.size >= 3)))
        }

        result.add(ActionRow.of(
            Button.success("obtain", "Obtain CC!").withDisabled(!obtainable),
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }

    private fun obtainCC(guild: Guild, event: IMessageEditCallback) {
        inventory.ccValidation.validationWay = validationWay

        inventory.ccValidation.cardList.addAll(seasonalCards)
        inventory.ccValidation.cardList.addAll(collaborationCards)
        inventory.ccValidation.cardList.addAll(t3Cards)

        seasonalCards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - 1 }
        collaborationCards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - 1 }
        t3Cards.forEach { c -> inventory.cards[c] = (inventory.cards[c] ?: 0) - 1 }

        CardBot.saveCardData()

        TransactionLogger.logCCObtain(authorMessage.author.idLong, inventory.ccValidation)

        val role = guild.roles.find { r -> r.id == CardData.cc }

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
                .setContent("CC obtained successfully! Check your role list")
                .setComponents()
                .setAllowedMentions(arrayListOf())
                .mentionRepliedUser(false)
                .queue()

            end(true)
        }
    }
}