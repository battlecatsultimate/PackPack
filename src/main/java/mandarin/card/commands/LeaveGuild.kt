package mandarin.card.commands

import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class LeaveGuild : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL)
            return

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(loader.channel, "Format : cd.lg [Guild ID]", loader.message) { a -> a }

            return
        }

        if (!StaticStore.isNumeric(contents[1])) {
            replyToMessageSafely(loader.channel, "Format : cd.lg [Guild ID]", loader.message) { a -> a }

            return
        }

        val id = StaticStore.safeParseLong(contents[1])

        val jda = loader.guild.jda

        val g = jda.guilds.find { g -> g.idLong == id }

        if (g == null) {
            replyToMessageSafely(loader.channel, "No such guild $id", loader.message) { a -> a }
        } else {
            g.leave().queue()

            replyToMessageSafely(loader.channel, "Left guild : ${g.name} - $id", loader.message) { a -> a }
        }
    }
}