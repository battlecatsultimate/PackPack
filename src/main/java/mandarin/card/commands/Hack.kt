package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.server.CommandLoader

class Hack : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        val inventory = Inventory.getInventory(m.idLong)

        CardData.cards.forEach { c ->
            inventory.cards[c] = (inventory.cards[c] ?: 0) + 1
        }

        ch.sendMessage("Prr").queue()
    }
}