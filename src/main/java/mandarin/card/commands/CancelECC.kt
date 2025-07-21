package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.CCCancelHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class CancelECC : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val member = loader.member

        if (!member.roles.any { r -> r.id == CardData.ecc }) {
            replyToMessageSafely(loader.channel, "You currently don't own ECC!", loader.message) { a -> a }

            return
        }

        val inventory = Inventory.getInventory(member.idLong)

        val builder = StringBuilder(
            "## Cancellation of ECC\n" +
                    "This is message that informs you about details of cancellation of ECC\n" +
                    "\n" +
                    "1. If there are cards that you have spent when validating ECC, you will retrieve them\n" +
                    "2. You can re-join ECC again at any time"
        )

        if (inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC }.isNotEmpty()) {
            builder.append("\n\nBelow lists are what you will retrieve :\n")

            inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC }.forEach { (card, pair) ->
                builder.append("- ").append(card.simpleCardInfo())

                if (pair.second >= 2) {
                    builder.append(" x").append(pair.second)
                }

                builder.append("\n")
            }
        }

        replyToMessageSafely(loader.channel, builder.toString(), loader.message, { a -> registerConfirmButtons(a, lang) }) { msg ->
            StaticStore.putHolder(member.id, CCCancelHolder(loader.message, member.id, loader.channel.id, msg, CCCancelHolder.CancelMode.ECC))
        }
    }
}