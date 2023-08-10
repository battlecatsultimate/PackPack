package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent

class Lock : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val m = getMember(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val ch = getChannel(event) ?: return

        if (CardBot.locked) {
            replyToMessageSafely(ch, "Bot is already locked. If you want to unlock the bot, you have to call `${CardBot.globalPrefix}unlock`", getMessage(event)) { a -> a }
        } else {
            CardBot.locked = true

            replyToMessageSafely(ch, "Bot is locked from users", getMessage(event)) { a -> a }
        }
    }
}