package mandarin.card.supporter.holder

import mandarin.card.CardBot
import mandarin.card.supporter.Card
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.modal.CraftAmountHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionComponent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.FileUpload

class CardCraftAmountHolder(author: Message, channelID: String, private val message: Message, private val craftMode: CardData.CraftMode) : ComponentHolder(author, channelID, message.id) {
    private val inventory = Inventory.getInventory(author.author.id)

    private var amount = 1

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        val name = when(craftMode) {
            CardData.CraftMode.T1 -> "Tier 1 [Common]"
            CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
            CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
            CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
            CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
            CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
        }

        when(event.componentId) {
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled craft")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Define amount of card that will be crafted")
                    .setRequired(true)
                    .setValue(amount.toString())
                    .build()

                val modal = Modal.create("amount", "Amount of Card")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, CraftAmountHolder(authorMessage, channelID, message.id) { a ->
                    amount = a

                    message.editMessage("You are crafting $amount $name card${if (amount >= 2) "s" else ""}\n" +
                                "\n" +
                                "You can change the amount of card that will be crafted as well\n" +
                                "\n" +
                                "Required shard : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${craftMode.cost * amount}\n" +
                                "Currently you have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}" +
                                if (amount * craftMode.cost > inventory.platinumShard) "\n\n**You can't craft cards because you don't have enough platinum shards!**" else ""
                        )
                        .setComponents(getComponents())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                })
            }
            "craft" -> {
                val components = ArrayList<ActionComponent>()

                components.add(Button.success("confirm", "Confirm"))
                components.add(Button.danger("cancel", "Cancel"))

                event.deferEdit()
                    .setContent("Are you sure you want to spend ${EmojiStore.ABILITY["SHARD"]?.formatted} ${craftMode.cost * amount} on crafting $amount $name card${if (amount >= 2) "s" else ""}?")
                    .setActionRow(components)
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                StaticStore.putHolder(authorMessage.author.id, ConfirmButtonHolder(authorMessage, message, channelID, {
                    val result = rollCards()

                    val builder = StringBuilder("### Craft Result [${result.size} cards in total]\n\n")

                    for (card in result) {
                        builder.append("- ")

                        if (card.tier == CardData.Tier.ULTRA) {
                            builder.append(Emoji.fromUnicode("✨").formatted).append(" ")
                        } else if (card.tier == CardData.Tier.LEGEND) {
                            builder.append(EmojiStore.ABILITY["LEGEND"]?.formatted).append(" ")
                        }

                        builder.append(card.cardInfo())

                        if (!inventory.cards.containsKey(card)) {
                            builder.append(" {**NEW**}")
                        }

                        if (card.tier == CardData.Tier.ULTRA) {
                            builder.append(" ").append(Emoji.fromUnicode("✨").formatted)
                        } else if (card.tier == CardData.Tier.LEGEND) {
                            builder.append(" ").append(EmojiStore.ABILITY["LEGEND"]?.formatted)
                        }

                        builder.append("\n")
                    }

                    event.messageChannel
                        .sendMessage(builder.toString())
                        .setMessageReference(authorMessage)
                        .addFiles(result.filter { c -> !inventory.cards.containsKey(c) }.map { c -> FileUpload.fromData(c.cardImage, "${c.name}.png") })
                        .mentionRepliedUser(false)
                        .queue()

                    inventory.addCards(result)
                    inventory.platinumShard -= amount * craftMode.cost

                    CardBot.saveCardData()

                    TransactionLogger.logCraft(authorMessage.author.idLong, amount, craftMode, result, amount.toLong() * craftMode.cost)
                }, LangID.EN))
            }
        }
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("amount", "Change amount of crafted card")))
        result.add(
            ActionRow.of(
                Button.success("craft", "Craft").withEmoji(Emoji.fromUnicode("\uD83E\uDE84")).withDisabled(amount * craftMode.cost > inventory.platinumShard),
                Button.danger("cancel", "Cancel")
            )
        )

        return result
    }

    private fun rollCards() : List<Card> {
        CardData.activatedBanners

        val cards = when(craftMode) {
            CardData.CraftMode.T1 -> CardData.cards.filter { c -> c.tier == CardData.Tier.COMMON }
            CardData.CraftMode.T2 -> CardData.cards.filter { c -> c.unitID in BannerFilter.Banner.TheAlimighties.getBannerData() || c.unitID in BannerFilter.Banner.GirlsAndMonsters.getBannerData() }
            CardData.CraftMode.SEASONAL -> CardData.cards.filter { c -> c.unitID in BannerFilter.Banner.Seasonal.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }
            CardData.CraftMode.COLLAB -> CardData.cards.filter { c -> c.unitID in BannerFilter.Banner.Collaboration.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }
            CardData.CraftMode.T3 -> CardData.cards.filter { c -> c.tier == CardData.Tier.ULTRA && c.unitID != 435 && c.unitID != 484 }
            CardData.CraftMode.T4 -> CardData.cards.filter { c -> c.tier == CardData.Tier.LEGEND }
        }

        val result = ArrayList<Card>()

        repeat(amount) {
            result.add(cards.random())
        }

        return result
    }
}