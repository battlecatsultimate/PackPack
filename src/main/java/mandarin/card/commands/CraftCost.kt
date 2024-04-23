package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.CraftCostHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class CraftCost : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (!CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        replyToMessageSafely(ch, getContent(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, CraftCostHolder(loader.message, ch.id, msg))
        }
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()
        
        result.add(ActionRow.of(Button.secondary("t2", "Regular Tier 2 [Uncommon]")))
        result.add(ActionRow.of(Button.secondary("seasonal", "Seasonal Tier 2 [Uncommon]")))
        result.add(ActionRow.of(Button.secondary("collab", "Collaboration Tier 2 [Uncommon]")))

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
        return "Select button to adjust the amount of ${EmojiStore.ABILITY["SHARD"]?.formatted} platinum shards that will be spent after crafting\n" +
                "\n" +
                "### Craft cost\n" +
                "\n" +
                "- Regular Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T2.cost}\n" +
                "- Seasonal Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.SEASONAL.cost}\n" +
                "- Collaboration Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.COLLAB.cost}\n" +
                "- Tier 3 [Ultra Rare (Exclusives)] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T3.cost}\n" +
                "- Tier 4 [Legend Rare] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.CraftMode.T4.cost}\n" +
                "\n" +
                "Page : 1 / 2"
    }
}