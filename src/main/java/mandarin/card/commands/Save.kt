package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Message.MentionType

class Save : Command(LangID.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        replyToMessageSafely(ch, "Saving...", loader.message, { a -> a }, { message ->
            CardBot.saveCardData()
            LogSession.session.saveSessionAsFile()

            message.editMessage("Done!")
                .setAllowedMentions(ArrayList<MentionType>())
                .mentionRepliedUser(false)
                .queue()
        })


    }
}