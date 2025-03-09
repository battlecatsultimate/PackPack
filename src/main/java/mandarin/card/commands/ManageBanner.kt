package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class ManageBanner : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        if (loader.user.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(loader.member)) {
            return
        }

        
    }
}