package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.events.message.GenericMessageEvent

class Save : Command(LangID.EN, false) {
    override fun doSomething(event: GenericMessageEvent?) {
        val m = getMember(event) ?: return
        val ch = getChannel(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val message = getRepliedMessageSafely(ch, "Saving...", getMessage(event)) { a -> a }

        CardBot.saveCardData()
        LogSession.session.saveSessionAsFile()

        message.editMessage("Done!")
            .setAllowedMentions(ArrayList<MentionType>())
            .mentionRepliedUser(false)
            .queue()
    }
}