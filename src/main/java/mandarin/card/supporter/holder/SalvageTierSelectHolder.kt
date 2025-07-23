package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu

class SalvageTierSelectHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Salvage expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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
                "t3" -> CardData.SalvageMode.T3
                else -> CardData.SalvageMode.T4
            }

            connectTo(event, CardSalvageHolder(authorMessage, userID, channelID, message, mode))
        } else if (event.componentId == "cancel") {
            event.deferEdit()
                .setContent("Canceled salvage")
                .setComponents()
                .setAllowedMentions(ArrayList())
                .mentionRepliedUser(false)
                .queue()

            end(true)
        }
    }

    override fun onBack(child: Holder) {
        message.editMessage("Select tier of the cards that will be salvaged")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<ActionRow>()

        val list = StringSelectMenu.create("tier")

        list.placeholder = "Select tier"

        list.addOptions(
            SelectOption.of("Tier 1 [Common]", "t1").withDescription("${CardData.SalvageMode.T1.cost} shard per card"),
            SelectOption.of("Regular Tier 2 [Uncommon]", "t2").withDescription("${CardData.SalvageMode.T2.cost} shards per card"),
            SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonalT2").withDescription("${CardData.SalvageMode.SEASONAL.cost} shards per card"),
            SelectOption.of("Collaboration Tier 2 [Uncommon]", "collaborationT2").withDescription("${CardData.SalvageMode.COLLAB.cost} shards per card"),
            SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3").withDescription("${CardData.SalvageMode.T3.cost} shards per card"),
            SelectOption.of("Tier 4 [Legend Rare]", "t4").withDescription("${CardData.SalvageMode.T4.cost} shards per card")
        )

        result.add(ActionRow.of(list.build()))

        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}