package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class Pool(private val tier: CardData.Tier) : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val pool = when(tier) {
            CardData.Tier.COMMON -> CardData.common
            CardData.Tier.UNCOMMON -> CardData.appendUncommon(CardData.uncommon)
            CardData.Tier.ULTRA -> CardData.appendUltra(CardData.ultraRare)
            CardData.Tier.LEGEND -> CardData.appendLR(CardData.legendRare)
            else -> return
        }

        replyToMessageSafely(ch, "Pool of ${CardData.tierCategoryText[tier.ordinal]}\n\n```${pool.joinToString(", ") { c -> c.unitID.toString() }}```", loader.message) { a -> a }
    }
}