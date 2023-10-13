package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SalvageTierSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Salvage : Command(LangID.EN, true) {
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
            SelectOption.of("Tier 1 [Common]", "t1").withDescription("${CardData.Tier.COMMON.cost} CF per card (minimum 10)"),
            SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3").withDescription("${CardData.Tier.ULTRA.cost} CF per card (1 card per call)")
        )

        result.add(ActionRow.of(list.build()))

        return result
    }
}