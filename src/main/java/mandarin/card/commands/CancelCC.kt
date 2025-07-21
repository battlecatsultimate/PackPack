package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.CCCancelHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class CancelCC : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val member = loader.member

        if (!member.roles.any { r -> r.id == CardData.cc }) {
            replyToMessageSafely(loader.channel, "You currently don't own CC!", loader.message) { a -> a }

            return
        }

        val cf = EmojiStore.ABILITY["CF"]?.formatted

        val builder = StringBuilder(
            "## Cancellation of CC\n" +
                    "This is message that informs you about details of cancellation of CC\n" +
                    "\n" +
                    "1. If there are cards or $cf that you have spent when validating CC, you will retrieve them\n" +
                    "2. If you also had ECC, ECC will be cancelled together because ECC requires users to have CC\n" +
                    "3. You can re-join CC or ECC again at any time"
        )

        val inventory = Inventory.getInventory(member.idLong)

        if (inventory.ccValidationWay != Inventory.CCValidationWay.LEGENDARY_COLLECTOR || (inventory.eccValidationWay != Inventory.ECCValidationWay.NONE && inventory.eccValidationWay != Inventory.ECCValidationWay.LEGENDARY_COLLECTOR)) {
            builder.append("\n\nBelow lists are what you will retrieve :\n")

            inventory.validationCards.entries.forEach { (card, pair) ->
                builder.append("- ").append(card.simpleCardInfo())

                if (pair.second >= 2) {
                    builder.append(" x").append(pair.second)
                }

                builder.append("\n")
            }

            val catFoods = when(inventory.ccValidationWay) {
                Inventory.CCValidationWay.SEASONAL_15,
                Inventory.CCValidationWay.COLLABORATION_12 -> 150000
                Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> 0
                Inventory.CCValidationWay.T3_3 -> 200000
                Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> 0
                Inventory.CCValidationWay.NONE -> 0
            }

            if (catFoods > 0) {
                builder.append("- ").append(cf).append(" ").append(catFoods)
            }
        }

        replyToMessageSafely(loader.channel, builder.toString(), loader.message, { a -> registerConfirmButtons(a, lang) }) { msg ->
            StaticStore.putHolder(member.id, CCCancelHolder(loader.message, member.id, loader.channel.id, msg, CCCancelHolder.CancelMode.CC))
        }
    }
}