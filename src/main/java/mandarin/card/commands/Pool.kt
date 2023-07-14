package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent

class Pool(private val tier: CardData.Tier) : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val pool = when(tier) {
            CardData.Tier.COMMON -> CardData.common
            CardData.Tier.UNCOMMON -> CardData.appendUncommon(CardData.uncommon)
            CardData.Tier.ULTRA -> CardData.ultraRare
            CardData.Tier.LEGEND -> CardData.appendLR(CardData.legendRare)
            else -> return
        }

        replyToMessageSafely(ch, "Pool of ${CardData.tierCategoryText[tier.ordinal]}\n\n```${pool.joinToString(", ") { c -> c.unitID.toString() }}```", getMessage(event)) { a -> a }
    }
}