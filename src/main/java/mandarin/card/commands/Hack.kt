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

        CardData.permanents.forEachIndexed { index, bannerSet -> bannerSet.forEach { banner -> CardData.bannerData[index][banner].forEach { id ->
            val card = CardData.cards.find { c -> c.id == id }

            if (card != null)
                inventory.cards[card] = (inventory.cards[card] ?: 0) + 1
        } } }

        ch.sendMessage("Prr").queue()
    }
}