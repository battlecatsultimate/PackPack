package mandarin.card.supporter.holder.card

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.holder.modal.CardIDHolder
import mandarin.card.supporter.holder.modal.CardNameHolder
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
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
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import kotlin.math.abs

class CardModifyHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card, private val createMode: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "id" -> {
                val id = TextInput.create("id", "ID", TextInputStyle.SHORT).setPlaceholder("Make sure not to conflict the ID!").build()

                val modal = Modal.create("cardID", "Card ID").addActionRow(id).build()

                event.replyModal(modal).queue()

                connectTo(CardIDHolder(authorMessage, userID, channelID, message, card, createMode))
            }
            "name" -> {
                val name = TextInput.create("name", "Name", TextInputStyle.SHORT).setPlaceholder("Type card name here").build()

                val modal = Modal.create("cardName", "Card Name").addActionRow(name).build()

                event.replyModal(modal).queue()

                connectTo(CardNameHolder(authorMessage, userID, channelID, message, card, createMode))
            }
            "banner" -> {
                connectTo(event, CardBannerSelectHolder(authorMessage, userID, channelID, message, card))
            }
            "image" -> {
                connectTo(event, CardFileHolder(authorMessage, userID, channelID, message, card))
            }
            "activate" -> {
                card.activated = !card.activated

                applyResult(event)
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                val oldTier = card.tier

                val tier = CardData.Tier.valueOf(event.values.first())

                if (tier == CardData.Tier.SPECIAL && card.tier != CardData.Tier.SPECIAL && CardData.cards.filter { c -> c.tier == CardData.Tier.SPECIAL }.any { c -> -c.id == card.id }) {
                    event.deferReply()
                        .setContent("T0 Cards can only contain negative IDs, and there's already a card with ID of ${-card.id}. Change card ID first!")
                        .setEphemeral(true)
                        .queue()

                    return
                } else if (tier != CardData.Tier.SPECIAL && card.tier == CardData.Tier.SPECIAL && CardData.cards.filter { c -> c.tier != CardData.Tier.SPECIAL }.any { c -> -c.id == card.id }) {
                    event.deferReply()
                        .setContent("Non-T0 Cards can only contain positive IDs, and there's already a card with ID of ${-card.id}. Change card ID first!")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                card.tier = tier

                if ((oldTier == CardData.Tier.SPECIAL && card.tier != CardData.Tier.SPECIAL) || (oldTier != CardData.Tier.SPECIAL && card.tier == CardData.Tier.SPECIAL)) {
                    card.id = -card.id
                }

                if (card.bcCard && card.tier == CardData.Tier.SPECIAL) {
                    card.bcCard = false
                }

                if (!createMode && oldTier != card.tier) {
                    val tierFolder = when(card.tier) {
                        CardData.Tier.SPECIAL -> "Tier 0"
                        CardData.Tier.COMMON -> "Tier 1"
                        CardData.Tier.UNCOMMON -> "Tier 2"
                        CardData.Tier.ULTRA -> "Tier 3"
                        CardData.Tier.LEGEND -> "Tier 4"
                        CardData.Tier.NONE -> throw IllegalStateException("E/CardModifyHolder::onEvent - Invalid tier ${card.tier} is assigned")
                    }

                    val cardFileName = "${abs(card.id)}-${card.name}"

                    val newPlace = File("./data/cards/$tierFolder/$cardFileName.png")

                    if (!card.cardImage.renameTo(newPlace)) {
                        event.deferReply()
                            .setContent("Failed to move image file to proper place...")
                            .setEphemeral(true)
                            .queue()

                        card.tier = oldTier

                        return
                    }

                    card.cardImage = newPlace
                }

                applyResult(event)
            }
            "type" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                card.cardType = Card.CardType.valueOf(event.values.first())

                if (card.cardType == Card.CardType.APRIL_FOOL) {
                    card.bcCard = false
                }

                applyResult(event)
            }
            "bcCard" -> {
                card.bcCard = !card.bcCard

                applyResult(event)
            }
            "tradable" -> {
                card.tradable = !card.tradable

                applyResult(event)
            }
            "create" -> {
                val tierFolder = when(card.tier) {
                    CardData.Tier.SPECIAL -> "Tier 0"
                    CardData.Tier.COMMON -> "Tier 1"
                    CardData.Tier.UNCOMMON -> "Tier 2"
                    CardData.Tier.ULTRA -> "Tier 3"
                    CardData.Tier.LEGEND -> "Tier 4"
                    CardData.Tier.NONE -> throw IllegalStateException("E/CardModifyHolder::onEvent - Invalid tier ${card.tier} is assigned")
                }

                val cardFileName = "${abs(card.id)}-${card.name}"

                val newPlace = File("./data/cards/$tierFolder/$cardFileName.png")

                if (!card.cardImage.renameTo(newPlace)) {
                    event.deferReply()
                        .setContent("Failed to move image file to proper place...")
                        .setEphemeral(true)
                        .queue()

                    return
                }

                card.cardImage = newPlace

                CardData.cards.add(card)

                CardBot.saveCardData()

                event.deferReply()
                    .setContent("Successfully created the card!")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "delete" -> {
                registerPopUp(event,
                    "Are you sure you want to delete this card? This operation will affect below, and cannot be undone :\n" +
                            "\n" +
                            "- Users' Inventories\n" +
                            "- Card Skin\n" +
                            "- Card Pack\n" +
                            "- Slot Machine\n" +
                            "- Products in `cd.buy`\n" +
                            "- Auctions"
                )

                connectTo(
                    ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        if(!card.cardImage.delete()) {
                            StaticStore.logger.uploadLog("W/CardModifyHolder::onEvent - Failed to delete the card image : ${card.cardImage.absolutePath}")

                            e.deferReply().setContent("Failed to delete card image! Aborting deletion of the card")
                                .setEphemeral(true)
                                .queue()

                            return@ConfirmPopUpHolder
                        }

                        CardData.inventories.values.forEach { i ->
                            i.cards.remove(card)
                            i.favorites.remove(card)
                            i.equippedSkins.remove(card)
                        }

                        CardData.skins.filter { s -> s.card == card }.forEach { s ->
                            CardData.inventories.values.forEach { i ->
                                i.skins.remove(s)
                            }
                        }

                        CardData.auctionSessions.filter { a -> a.card == card }.forEach { a ->
                            a.cancelSession(-1L)
                        }

                        CardData.cardPacks.forEach { p ->
                            var affected = false

                            p.cost.cardsCosts.filterIsInstance<SpecificCardCost>().forEach { c ->
                                if (c.cards.contains(card)) {
                                    affected = true
                                }

                                c.cards.remove(card)
                            }

                            p.cost.cardsCosts.removeIf { c -> c is SpecificCardCost && c.cards.isEmpty() }

                            if (affected) {
                                p.activated = false
                            }
                        }

                        CardData.cards.remove(card)

                        CardBot.saveCardData()

                        e.deferReply()
                            .setContent("Successfully removed the card!")
                            .setEphemeral(true)
                            .queue()

                        goBack()
                    }, CommonStatic.Lang.Locale.EN)
                )
            }
            "confirm" -> {
                goBack(event)
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creating the card? This cannot be undone")

                connectTo(
                    ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        if (!card.cardImage.delete()) {
                            StaticStore.logger.uploadLog("W/CardModifyHolder::onEvent - Failed to delete card image : ${card.cardImage.absolutePath}")
                        }

                        goBack(e)
                    }, CommonStatic.Lang.Locale.EN)
                )
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

    override fun onConnected(parent: Holder?) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult(event: IMessageEditCallback) {
        var editor = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)

        if (message.attachments.isEmpty()) {
            editor = editor.setFiles(FileUpload.fromData(card.cardImage))
        }

        editor.queue()
    }

    private fun applyResult() {
        var editor = message.editMessage(getContents())
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)

        if (message.attachments.isEmpty()) {
            editor = editor.setFiles(FileUpload.fromData(card.cardImage))
        }

        editor.queue()
    }

    private fun getContents() : String {
        val active = if (card.activated) {
            EmojiStore.SWITCHON.formatted + " Yes"
        } else {
            EmojiStore.SWITCHOFF.formatted + " No"
        }

        val tierName = when(card.tier) {
            CardData.Tier.SPECIAL -> "Special [T0]"
            CardData.Tier.COMMON -> "Common [T1]"
            CardData.Tier.UNCOMMON -> "Uncommon [T2]"
            CardData.Tier.ULTRA -> "Ultra Rare [T3]"
            CardData.Tier.LEGEND -> "Legendary [T4]"
            CardData.Tier.NONE -> "None"
        }

        val typeName = when(card.cardType) {
            Card.CardType.NORMAL -> "Normal"
            Card.CardType.COLLABORATION -> "Collaboration"
            Card.CardType.SEASONAL -> "Seasonal"
            Card.CardType.APRIL_FOOL -> "April Fools"
        }

        val bannerName = if (card.banner.isEmpty()) {
            "None"
        } else {
            card.banner.joinToString { b -> b.name }
        }

        val bcCard = if (card.bcCard) {
            "Yes"
        } else {
            "No"
        }

        var text = "## ${card.name}\n" +
                "- **ID** : ${card.id}\n" +
                "- **Tier** : $tierName\n" +
                "- **Type** : $typeName\n" +
                "- **Banner** : $bannerName\n" +
                "- **Is BC Card** : $bcCard\n" +
                "- **Is Activated** : $active"

        if (CardData.cards.any { c -> c !== card && c.id == card.id }) {
            text += "\n\n**Conflict** : There's a card that has same ID of this card!"
        }

        return text
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val activated = if (card.activated) {
            EmojiStore.SWITCHON
        } else {
            EmojiStore.SWITCHOFF
        }

        val bcCard = if (card.bcCard) {
            EmojiStore.SWITCHON
        } else {
            EmojiStore.SWITCHOFF
        }

        val tradable = if (card.tradable) {
            EmojiStore.SWITCHON
        } else {
            EmojiStore.SWITCHOFF
        }

        result.add(ActionRow.of(
            Button.secondary("id", "Change Card ID").withEmoji(Emoji.fromUnicode("🆔")),
            Button.secondary("name", "Change Card Name").withEmoji(Emoji.fromUnicode("🏷️")),
            Button.secondary("activate", "Activate the Card").withEmoji(activated)
        ))

        result.add(ActionRow.of(
            Button.secondary("banner", "Change Banner").withEmoji(Emoji.fromUnicode("🗂️")),
            Button.secondary("image", "Change Card Image").withEmoji(EmojiStore.PNG),
            Button.secondary("bcCard", "Toggle BC Card").withEmoji(bcCard).withDisabled(card.tier == CardData.Tier.SPECIAL),
            Button.secondary("tradable", "Tradable").withEmoji(tradable)
        ))

        val possibleTiers = CardData.Tier.entries.filter { t -> t != CardData.Tier.NONE }

        val tierOptions = possibleTiers.map { t ->
            val label = when(t) {
                CardData.Tier.SPECIAL -> "Special [T0]"
                CardData.Tier.COMMON -> "Common [T1]"
                CardData.Tier.UNCOMMON -> "Uncommon [T2]"
                CardData.Tier.ULTRA -> "Ultra Rare [T3]"
                CardData.Tier.LEGEND -> "Legendary [T4]"
                CardData.Tier.NONE -> "None"
            }

            SelectOption.of(label, t.name).withDefault(card.tier == t)
        }

        result.add(ActionRow.of(StringSelectMenu.create("tier").addOptions(tierOptions).build()))

        val typeOptions = Card.CardType.entries.map { t ->
            val label = when(t) {
                Card.CardType.NORMAL -> "Normal"
                Card.CardType.COLLABORATION -> "Collaboration"
                Card.CardType.SEASONAL -> "Seasonal"
                Card.CardType.APRIL_FOOL -> "April Fools"
            }

            SelectOption.of(label, t.name).withDefault(card.cardType == t)
        }

        result.add(ActionRow.of(StringSelectMenu.create("type").addOptions(typeOptions).build()))

        if (createMode) {
            result.add(ActionRow.of(
                Button.success("create", "Create").withEmoji(EmojiStore.CHECK).withDisabled(CardData.cards.any { c -> c !== card && c.id == card.id }),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.primary("confirm", "Confirm").withEmoji(EmojiStore.BACK),
                Button.danger("delete", "Delete").withEmoji(Emoji.fromUnicode("🗑️"))
            ))
        }

        return result
    }
}