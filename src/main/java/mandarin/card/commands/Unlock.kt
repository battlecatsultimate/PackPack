package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent

class Unlock : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val m = getMember(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val ch = getChannel(event) ?: return

        if ("-roll" in getContent(event).split(" ")) {
            if (CardBot.rollLocked) {
                CardBot.rollLocked = false

                replyToMessageSafely(ch, "Roll command is unlocked, and users can roll cards freely", getMessage(event)) { a -> a }
            } else {
                replyToMessageSafely(ch, "Roll command is already unlocked. If you want to lock the bot, you have to call `${CardBot.globalPrefix}lock -roll`", getMessage(event)) { a -> a }
            }

            return
        }

        if (!CardBot.locked) {
            replyToMessageSafely(ch, "Bot is already unlocked. If you want to lock the bot, you have to call `${CardBot.globalPrefix}lock`", getMessage(event)) { a -> a }
        } else {
            CardBot.locked = false

            replyToMessageSafely(ch, "Bot is unlocked, and users can use the bot again", getMessage(event)) { a -> a }
        }
    }
}