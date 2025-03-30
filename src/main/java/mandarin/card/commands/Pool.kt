package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class Pool(private val tier: CardData.Tier) : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val pool = when(tier) {
            CardData.Tier.COMMON -> CardData.common
            CardData.Tier.UNCOMMON -> CardData.appendUncommon()
            CardData.Tier.ULTRA -> CardData.appendUltra()
            CardData.Tier.LEGEND -> CardData.appendLR()
            else -> return
        }

        replyToMessageSafely(ch, "Pool of ${CardData.tierCategoryText[tier.ordinal]}\n\n```${pool.joinToString(", ") { c -> c.id.toString() }}```", loader.message) { a -> a }
    }
}