package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.CardComparator
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class SalvageTierSelectHolder(author: Message, channelID: String, private val message: Message) : ComponentHolder(author, channelID, message.id) {
    val inventory = Inventory.getInventory(author.author.id)

    override fun clean() {

    }

    override fun onExpire(id: String) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        if (event.componentId == "tier") {
            if (event !is StringSelectInteractionEvent)
                return

            if (event.values.isEmpty())
                return

            val mode = when(event.values[0]) {
                "t1" -> CardData.SalvageMode.T1
                "t2" -> CardData.SalvageMode.T2
                "seasonalT2" -> CardData.SalvageMode.SEASONAL
                "collaborationT2" -> CardData.SalvageMode.COLLAB
                else -> CardData.SalvageMode.T3
            }

            connectTo(event, CardSalvageHolder(authorMessage, channelID, message, mode))
        } else if (event.componentId == "cancel") {
            event.deferEdit()
                .setContent("Canceled salvage")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            expired = true

            expire(authorMessage.author.id)
        }
    }

    override fun onBack() {
        message.editMessage("Select tier of the cards that will be salvaged")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<ActionRow>()

        val list = StringSelectMenu.create("tier")

        list.placeholder = "Select tier"

        list.addOptions(
            SelectOption.of("Tier 1 [Common]", "t1").withDescription("${CardData.SalvageMode.T1.cost} shard per card"),
            SelectOption.of("Regular Tier 2 [Uncommon]", "t2").withDescription("${CardData.SalvageMode.T2.cost} shards per card"),
            SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonalT2").withDescription("${CardData.SalvageMode.SEASONAL.cost} shards per card"),
            SelectOption.of("Collaboration Tier 2 [Uncommon]", "collaborationT2").withDescription("${CardData.SalvageMode.COLLAB.cost} shards per card"),
            SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3").withDescription("${CardData.SalvageMode.T3.cost} shards per card")
        )

        result.add(ActionRow.of(list.build()))

        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}