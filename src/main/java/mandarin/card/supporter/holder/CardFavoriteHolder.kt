package mandarin.card.supporter.holder

import common.util.Data
import mandarin.card.CardBot
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.CardFavoriteAmountHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageUpdater
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
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
import net.dv8tion.jda.api.utils.FileUpload
import kotlin.math.min

class CardFavoriteHolder(author: Message, channelID: String, private var message: Message, private val inventory: Inventory, private val card: Card) : ComponentHolder(author, channelID, message), MessageUpdater {
    private val skins = inventory.skins.filter { s -> s.card == card }

    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

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
            "skin" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                if (event.values.isEmpty()) {
                    inventory.equippedSkins.remove(card)

                    event.deferReply()
                        .setContent("Successfully unequipped the skin!")
                        .setEphemeral(true)
                        .queue()
                } else {
                    val index = event.values.first().toInt()
                    val skin = skins[index]

                    val oldSkin = inventory.equippedSkins.put(card, skin)

                    if (oldSkin == null) {
                        event.deferReply()
                            .setContent("Successfully equipped the skin : ${skin.name}!")
                            .setEphemeral(true)
                            .queue()
                    } else {
                        event.deferReply()
                            .setContent("Successfully changed the skin : ${skin.name}!")
                            .setEphemeral(true)
                            .queue()
                    }
                }

                applyResult()
            }
            "favorite" -> {
                val amount = inventory.cards[card] ?: 0

                if (amount >= 2) {
                    val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                        .setPlaceholder("Favorite cards up to ${inventory.cards[card] ?: 1} card(s)")
                        .setRequired(true)
                        .build()

                    val modal = Modal.create("favorite", "Favorite Cards")
                        .addActionRow(input)
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardFavoriteAmountHolder(authorMessage, channelID, message, inventory, card, true))
                } else {
                    inventory.favoriteCards(card, 1)

                    event.deferReply()
                        .setContent("Successfully added this card into favorite list!")
                        .setEphemeral(true)
                        .queue()

                    applyResult()
                }

                CardBot.saveCardData()
            }
            "unfavorite" -> {
                val amount = inventory.favorites[card] ?: 0

                if (amount >= 2) {
                    val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                        .setPlaceholder("Unfavorite cards up to ${inventory.favorites[card] ?: 1} card(s)")
                        .setRequired(true)
                        .build()

                    val modal = Modal.create("favorite", "Unfavorite Cards")
                        .addActionRow(input)
                        .build()

                    event.replyModal(modal).queue()

                    connectTo(CardFavoriteAmountHolder(authorMessage, channelID, message, inventory, card, false))
                } else {
                    inventory.unfavoriteCards(card, 1)

                    event.deferReply()
                        .setContent("Successfully removed this card from favorite list!")
                        .setEphemeral(true)
                        .queue()

                    applyResult()
                }

                CardBot.saveCardData()
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
            }
        }
    }

    override fun onMessageUpdated(message: Message) {
        this.message = message

        println(message.attachments)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        var builder =  event.deferEdit()
            .setContent(getContents()).setEmbeds(getEmbed())
            .setComponents(getComponents())
            .mentionRepliedUser(false)

        val equippedSkin = inventory.equippedSkins[card]

        if (equippedSkin == null) {
            builder = builder.setFiles(FileUpload.fromData(card.cardImage, "card.png"))
        } else {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun applyResult() {
        var builder = message.editMessageEmbeds(getEmbed())
            .setContent(getContents())
            .setComponents(getComponents())
            .mentionRepliedUser(false)

        val equippedSkin = inventory.equippedSkins[card]

        builder = if (equippedSkin == null) {
            builder.setFiles(FileUpload.fromData(card.cardImage, "card.png"))
        } else {
            builder.setFiles()
        }

        builder.queue()
    }

    private fun getContents() : String {
        return if (skins.isEmpty()) {
            "You can favorite card to prevent it from being spent accidentally"
        } else {
            "You can favorite card to prevent it from being spent accidentally\n" +
            "\n" +
            "Select skin to equip purchased skins. You can unequip skin by selecting equipped skin again"
        }
    }

    private fun getEmbed() : MessageEmbed {
        val embedBuilder = EmbedBuilder()
        val equippedSkin = inventory.equippedSkins[card]

        val favorite = if (inventory.favorites.containsKey(card)) " ⭐ " else ""

        embedBuilder.setTitle(favorite + "Card No.${Data.trio(card.unitID)}" + favorite)

        embedBuilder.setColor(CardData.grade[card.tier.ordinal])

        embedBuilder.addField("Name", card.name, true)
        embedBuilder.addField("Tier", card.getTier(), true)

        embedBuilder.addField("Amount", (inventory.cards[card] ?: 0).toString(), false)

        if (inventory.favorites.containsKey(card)) {
            embedBuilder.addField("Favorite", (inventory.favorites[card] ?: 0).toString(), false)
        }

        if (equippedSkin != null) {
            embedBuilder.addField("Skin", equippedSkin.name, false)

            if (equippedSkin.cacheLink.isEmpty())
                equippedSkin.cache(authorMessage.jda, true)

            embedBuilder.setImage(equippedSkin.cacheLink)
        } else {
            embedBuilder.setImage("attachment://card.png")
        }

        return embedBuilder.build()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(
            ActionRow.of(
                Button.secondary("favorite", "Add to Favorite").withEmoji(Emoji.fromUnicode("⭐")).withDisabled(!inventory.cards.containsKey(card)),
                Button.secondary("unfavorite", "Remove from Favorite").withEmoji(Emoji.fromUnicode("❌")).withDisabled(!inventory.favorites.containsKey(card))
            )
        )

        if (skins.isNotEmpty()) {
            val equippedSkin = inventory.equippedSkins[card]

            val options = ArrayList<SelectOption>()

            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {

                options.add(SelectOption.of(skins[i].name, i.toString()).withDescription(skins[i].skinID.toString()).withDefault(skins[i] == equippedSkin))
            }

            result.add(ActionRow.of(
                StringSelectMenu.create("skin")
                    .setPlaceholder("Select Skin To Equip")
                    .addOptions(options)
                    .setRequiredRange(0, 1)
                    .build()
            ))

            if (skins.size > SearchHolder.PAGE_CHUNK) {
                val buttons = ArrayList<Button>()
                val totalPage = getTotalPage(skins.size)

                if (totalPage > 10) {
                    buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
                }

                buttons.add(Button.secondary("prev", "Previous Pages").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

                buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

                if (totalPage > 10) {
                    buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
                }

                result.add(ActionRow.of(buttons))
            }
        }

        result.add(
            ActionRow.of(
                Button.secondary("back", "Go Back")
            )
        )

        return result
    }
}