package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class SendMessage : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid")) {
            return
        }

        val contents = loader.content.split(Regex(" "), 4)

        if (contents.size != 4) {
            replyToMessageSafely(loader.channel, "`cd.sm [Guild ID] [Channel ID] [Content]`", loader.message) { a -> a }

            return
        }

        val g = loader.client.getGuildById(contents[1]) ?: return

        val channel = g.getGuildChannelById(contents[2]) ?: return

        if (channel !is GuildMessageChannel)
            return

        val content = if (contents[3].startsWith("`"))
            contents[3].substring(1, contents[3].length - 1).replace(Regex("\\e"), "")
        else
            contents[3].replace(Regex("\\e"), "")

        channel.sendMessage(content).queue()
        replyToMessageSafely(loader.channel, content, loader.message) { a -> a }
    }
}