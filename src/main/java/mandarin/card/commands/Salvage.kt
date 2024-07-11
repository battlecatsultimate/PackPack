package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SalvageTierSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Salvage : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        replyToMessageSafely(ch, "Select tier of the cards that will be salvaged", loader.message, { a ->
            a.setComponents(assignComponents())
        }, { message ->
            StaticStore.putHolder(m.id, SalvageTierSelectHolder(loader.message, ch.id, message))
        })
    }

    private fun assignComponents() : List<ActionRow> {
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