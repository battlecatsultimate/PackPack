package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class Lock : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val ch = loader.channel

        if ("-roll" in loader.content.split(" ")) {
            if (!CardBot.rollLocked) {
                CardBot.rollLocked = true

                replyToMessageSafely(ch, "Roll command is locked from users", loader.message) { a -> a }
            } else {
                replyToMessageSafely(ch, "Roll command is already locked. If you want to unlock the bot, you have to call `${CardBot.globalPrefix}unlock -roll`", loader.message) { a -> a }
            }

            return
        }

        if (CardBot.locked) {
            replyToMessageSafely(ch, "Bot is already locked. If you want to unlock the bot, you have to call `${CardBot.globalPrefix}unlock`", loader.message) { a -> a }
        } else {
            CardBot.locked = true

            replyToMessageSafely(ch, "Bot is locked from users", loader.message) { a -> a }
        }
    }
}