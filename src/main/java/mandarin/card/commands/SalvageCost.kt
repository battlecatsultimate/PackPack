package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SalvageCostHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class SalvageCost : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        replyToMessageSafely(ch, getContent(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, SalvageCostHolder(loader.message, ch.id, msg))
        }
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("t1", "Tier 1 [Common]")))
        result.add(ActionRow.of(Button.secondary("t2", "Regular Tier 2 [Uncommon]")))
        result.add(ActionRow.of(Button.secondary("seasonal", "Seasonal Tier 2 [Uncommon]")))

        result.add(
            ActionRow.of(
                Button.secondary("prev", "Previous").withEmoji(EmojiStore.PREVIOUS).asDisabled(),
                Button.secondary("next", "Next").withEmoji(EmojiStore.NEXT)
            )
        )

        result.add(ActionRow.of(Button.primary("confirm", "Confirm")))

        return result
    }

    private fun getContent() : String {
        return "Select button to adjust the amount of ${EmojiStore.ABILITY["SHARD"]?.formatted} platinum shards that will be given after salvaging\n" +
                "\n" +
                "### Salvage cost\n" +
                "\n" +
                "- Tier 1 [Common] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T1.cost}\n" +
                "- Regular Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T2.cost}\n" +
                "- Seasonal Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.SEASONAL.cost}\n" +
                "- Collaboration Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.COLLAB.cost}\n" +
                "- Tier 3 [Ultra Rare (Exclusives)] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T3.cost}\n" +
                "\n" +
                "Page : 1 / 2"
    }
}