package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class Notice : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id in CardData.notifierGroup) {
            CardData.notifierGroup.remove(m.id)

            CardBot.saveCardData()

            replyToMessageSafely(ch, "Bot now won't notify you even though you can roll the packs!", loader.message) { a -> a }

            return
        }

        m.user.openPrivateChannel().queue({ private ->
            private.sendMessage("This is testing message if bot can reach to your DM").queue( { _ ->
                CardData.notifierGroup.add(m.id)

                CardBot.saveCardData()

                replyToMessageSafely(ch, "Bot will now notify if you can roll any packs from now on! Keep in mind that closing DM will automatically make bot not notify you anymore. Call this command again to make bot not notify you manually", loader.message) { a -> a }
            }, { _ ->
                replyToMessageSafely(ch, "Bot failed to send test message in your DM. Please check if your DM is opened, or contact managers", loader.message) { a -> a }
            })
        }, { _ ->
            replyToMessageSafely(ch, "Please open your DM to get notified", loader.message) { a -> a }
        })
    }
}