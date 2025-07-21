package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.CardBot
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

class CCValidationHolder(author: Message, userID: String, channelID: String, message: Message, private val validationWay: Inventory.CCValidationWay) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private val seasonalCards = ArrayList<Card>()
    private val collaborationCards = ArrayList<Card>()
    private val t3Cards = ArrayList<Card>()

    init {
        registerAutoExpiration(FIVE_MIN)

        seasonalCards.addAll(inventory.validationCards.filterKeys { c -> c.cardType == Card.CardType.SEASONAL }.keys.toSet())
        collaborationCards.addAll(inventory.validationCards.filterKeys { c -> c.cardType == Card.CardType.COLLABORATION }.keys.toSet())
        t3Cards.addAll(inventory.validationCards.filterKeys { c -> c.tier == CardData.Tier.ULTRA }.keys.toSet())
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val guild = event.jda.getGuildById(CardData.guild) ?: return

        when(event.componentId) {
            "seasonal" -> {
                val cards = inventory.cards.keys.filter { c -> c.cardType == Card.CardType.SEASONAL }.sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, seasonalCards, 15))
            }
            "collaboration" -> {
                val cards = inventory.cards.keys.filter { c -> c.cardType == Card.CardType.COLLABORATION}.sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, collaborationCards, 12))
            }
            "t3" -> {
                val cards = inventory.cards.keys.filter { c -> c.tier == CardData.Tier.ULTRA }.sortedWith(CardComparator())

                connectTo(event, ValidationPayHolder(authorMessage, userID, channelID, message, cards, t3Cards, 3))
            }
            "obtain" -> {
                if (validationWay != Inventory.CCValidationWay.LEGENDARY_COLLECTOR) {
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
        val doable = Inventory.checkCCDoable(validationWay, inventory)

        if (doable.isNotBlank()) {
            return "You can't validate CC with this way because of the reasons below! :\n\n$doable"
        }

        if (validationWay == Inventory.CCValidationWay.LEGENDARY_COLLECTOR) {
            return "Are you sure you want to obtain CC with this way? **Keep in mind that your CC will be lost if you lose <@&${CardData.Role.LEGEND.id}>**"
        }

        val builder = StringBuilder("Please check validation status below :\n\n")
        val cf = EmojiStore.ABILITY["CF"]?.formatted

        when(validationWay) {
            Inventory.CCValidationWay.SEASONAL_15 -> {
                val seasonal = if (seasonalCards.size < 15) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 15 Seasonal Cards : ").append(seasonal).append("\n").append("- $cf 150000 : ⭕")
            }
            Inventory.CCValidationWay.COLLABORATION_12 -> {
                val collaboration = if (collaborationCards.size < 12) {
                    "❌"
                } else {
                    "⭕"
                }

                builder.append("- Unique 12 Collaboration Cards : ").append(collaboration).append("\n").append("- $cf 150000 : ⭕")
            }
            Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> {
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
            Inventory.CCValidationWay.T3_3 -> {
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

        if (Inventory.checkCCDoable(validationWay, inventory).isNotBlank()) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))

            return result
        }

        if (validationWay == Inventory.CCValidationWay.LEGENDARY_COLLECTOR) {
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
            Inventory.CCValidationWay.SEASONAL_15 -> {
                needSeasonal = true

                obtainable = seasonalCards.size >= 15
            }
            Inventory.CCValidationWay.COLLABORATION_12 -> {
                needCollaboration = true

                obtainable = collaborationCards.size >= 12
            }
            Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> {
                needSeasonal = true
                needCollaboration = true

                obtainable = seasonalCards.size >= 15 && collaborationCards.size >= 12
            }
            Inventory.CCValidationWay.T3_3 -> {
                needT3 = true

                obtainable = t3Cards.size >= 3
            }
            else -> throw IllegalStateException("E/CCValidationHolder::getComponents - Unhandled validation way : $validationWay")
        }

        if (needSeasonal) {
            result.add(ActionRow.of(Button.secondary("seasonal", "Pay Seasonal Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL))))
        }

        if (needCollaboration) {
            result.add(ActionRow.of(Button.secondary("collaboration", "Pay Collaboration Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION))))
        }

        if (needT3) {
            result.add(ActionRow.of(Button.secondary("t3", "Pay T3 Cards").withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T3))))
        }

        result.add(ActionRow.of(
            Button.success("obtain", "Obtain CC!").withDisabled(!obtainable).withEmoji(EmojiStore.CC),
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }

    private fun obtainCC(guild: Guild, event: IMessageEditCallback) {
        inventory.ccValidationWay = validationWay

        seasonalCards.union(collaborationCards).union(t3Cards).forEach { c ->
            val pair = inventory.validationCards[c]

            if (pair == null) {
                inventory.validationCards[c] = Pair(Inventory.ShareStatus.CC, 1)

                inventory.cards[c] = (inventory.cards[c] ?: 0) - 1
            } else if (pair.first == Inventory.ShareStatus.ECC) {
                inventory.validationCards[c] = Pair(Inventory.ShareStatus.BOTH, pair.second)
            }
        }

        when(validationWay) {
            Inventory.CCValidationWay.SEASONAL_15,
            Inventory.CCValidationWay.COLLABORATION_12 -> inventory.catFoods -= 150000
            Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> {}
            Inventory.CCValidationWay.T3_3 -> inventory.catFoods -= 200000
            Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> {}
            Inventory.CCValidationWay.NONE -> {}
        }

        CardBot.saveCardData()

        TransactionLogger.logCCObtain(authorMessage.author.idLong, validationWay, seasonalCards.union(collaborationCards).union(t3Cards).toList())

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