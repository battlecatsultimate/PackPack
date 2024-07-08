package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class CardCraftModeHolder(author: Message, channelID: String, message: Message) : ComponentHolder(author, channelID, message) {
    val inventory = Inventory.getInventory(author.author.idLong)

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled craft")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedMode = when(event.values[0]) {
                    "t2" -> CardData.CraftMode.T2
                    "seasonal" -> CardData.CraftMode.SEASONAL
                    "collab" -> CardData.CraftMode.COLLAB
                    "t3" -> CardData.CraftMode.T3
                    else -> CardData.CraftMode.T4
                }

                val emoji = when(selectedMode) {
                    CardData.CraftMode.T2 -> EmojiStore.getCardEmoji(CardPack.CardType.T2)
                    CardData.CraftMode.SEASONAL -> EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL)
                    CardData.CraftMode.COLLAB -> EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION)
                    CardData.CraftMode.T3 -> EmojiStore.getCardEmoji(CardPack.CardType.T3)
                    CardData.CraftMode.T4 -> EmojiStore.getCardEmoji(CardPack.CardType.T4)
                }

                val name = (emoji?.formatted ?: "") + " " + when(selectedMode) {
                    CardData.CraftMode.T2 -> "Regular Tier 2 [Uncommon]"
                    CardData.CraftMode.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                    CardData.CraftMode.COLLAB -> "Collaboration Tier 2 [Uncommon]"
                    CardData.CraftMode.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                    CardData.CraftMode.T4 -> "Tier 4 [Legend Rare]"
                }

                event.deferEdit()
                    .setContent("You are crafting 1 $name card\n" +
                            "\n" +
                            "You can change the amount of card that will be crafted as well\n" +
                            "\n" +
                            "Required shard : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${selectedMode.cost}\n" +
                            "Currently you have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}" +
                            if (selectedMode.cost > inventory.platinumShard) "\n\n**You can't craft cards because you don't have enough platinum shards!**" else "")
                    .setComponents(getComponents(selectedMode))
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                StaticStore.putHolder(authorMessage.author.id, CardCraftAmountHolder(authorMessage, channelID, message, selectedMode))
            }
        }
    }

    private fun getComponents(craftMode: CardData.CraftMode) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("reduce", "Reduce Amount").asDisabled().withEmoji(Emoji.fromUnicode("➖")),
            Button.secondary("amount", "Set Amount"),
            Button.secondary("add", "Add Amount").withEmoji(Emoji.fromUnicode("➕"))
        ))

        result.add(
            ActionRow.of(
                Button.success("craft", "Craft").withEmoji(Emoji.fromUnicode("\uD83E\uDE84")).withDisabled(craftMode.cost > inventory.platinumShard),
                Button.danger("cancel", "Cancel")
            )
        )

        return result
    }
}