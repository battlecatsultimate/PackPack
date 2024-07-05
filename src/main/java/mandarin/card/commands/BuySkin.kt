package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.LayoutComponent

class BuySkin : Command(LangID.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val cards = CardData.skins.map { s -> s.card }.toHashSet().toMutableList()
    }
}