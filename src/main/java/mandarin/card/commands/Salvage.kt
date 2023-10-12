package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SalvageTierSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Salvage : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return

        if (CardBot.rollLocked && !CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val message = getRepliedMessageSafely(ch, "Select tier of the cards that will be salvaged", getMessage(event)) { a -> a.setComponents(assignComponents()) }

        StaticStore.putHolder(m.id, SalvageTierSelectHolder(getMessage(event), ch.id, message))
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