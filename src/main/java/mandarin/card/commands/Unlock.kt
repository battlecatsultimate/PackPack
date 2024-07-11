package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class Unlock : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val ch = loader.channel

        if ("-roll" in loader.content.split(" ")) {
            if (CardBot.rollLocked) {
                CardBot.rollLocked = false

                replyToMessageSafely(ch, "Roll command is unlocked, and users can roll cards freely", loader.message) { a -> a }
            } else {
                replyToMessageSafely(ch, "Roll command is already unlocked. If you want to lock the bot, you have to call `${CardBot.globalPrefix}lock -roll`", loader.message) { a -> a }
            }

            return
        }

        if (!CardBot.locked) {
            replyToMessageSafely(ch, "Bot is already unlocked. If you want to lock the bot, you have to call `${CardBot.globalPrefix}lock`", loader.message) { a -> a }
        } else {
            CardBot.locked = false

            replyToMessageSafely(ch, "Bot is unlocked, and users can use the bot again", loader.message) { a -> a }
        }
    }
}