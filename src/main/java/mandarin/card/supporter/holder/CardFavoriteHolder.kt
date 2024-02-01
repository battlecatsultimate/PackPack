package mandarin.card.supporter.holder

import common.util.Data
import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.CardFavoriteAmountHolder
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload

class CardFavoriteHolder(author: Message, channelID: String, private val message: Message, private val inventory: Inventory, private val card: Card) : ComponentHolder(author, channelID, message.id) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
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

                    connectTo(CardFavoriteAmountHolder(authorMessage, channelID, message, inventory, card, true))
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

    override fun onBack() {
        super.onBack()

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent("You can favorite card to prevent it from being spent accidentally")
            .setEmbeds(getEmbed())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setFiles(FileUpload.fromData(card.cardImage, "card.png"))
            .queue()
    }

    private fun applyResult() {
        message.editMessageEmbeds(getEmbed())
            .setContent("You can favorite card to prevent it from being spent accidentally")
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getEmbed() : MessageEmbed {
        val embedBuilder = EmbedBuilder()

        val favorite = if (inventory.favorites.containsKey(card)) " ⭐ " else ""

        embedBuilder.setTitle(favorite + "Card No.${Data.trio(card.unitID)}" + favorite)

        embedBuilder.setColor(StaticStore.grade[card.tier.ordinal])

        embedBuilder.addField("Name", card.name, true)
        embedBuilder.addField("Tier", card.getTier(), true)

        embedBuilder.addField("Amount", (inventory.cards[card] ?: 0).toString(), false)

        if (inventory.favorites.containsKey(card)) {
            embedBuilder.addField("Favorite", (inventory.favorites[card] ?: 0).toString(), false)
        }

        embedBuilder.setImage("attachment://card.png")

        return embedBuilder.build()
    }

    private fun getComponents() : List<LayoutComponent> {
        val components = ArrayList<LayoutComponent>()

        components.add(
            ActionRow.of(
                Button.secondary("favorite", "Add to Favorite").withEmoji(Emoji.fromUnicode("⭐")).withDisabled(!inventory.cards.containsKey(card)),
                Button.secondary("unfavorite", "Remove from Favorite").withEmoji(Emoji.fromUnicode("❌")).withDisabled(!inventory.favorites.containsKey(card))
            )
        )

        components.add(
            ActionRow.of(
                Button.secondary("back", "Go Back")
            )
        )

        return components
    }
}