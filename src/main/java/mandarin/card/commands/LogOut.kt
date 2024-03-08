package mandarin.card.commands


import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore

import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel

class LogOut : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        if (CardBot.test)
            return

        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid"))
            return

        val ch = loader.channel
        val client = ch.jda.shardManager ?: return

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "Format : `cd.lo -b/-u/-a/-m`\n" +
                    "\n" +
                    "-b : Bug fix\n" +
                    "-u : Update\n" +
                    "-a : API Update\n" +
                    "-m : Maintenance\n", loader.message) { a -> a }

            return
        }

        val reason = when(contents[1]) {
            "-b" -> "bug fix"
            "-u" -> "update"
            "-a" -> "API update"
            "-m" -> "maintenance"
            else -> ""
        }

        if (reason.isEmpty()) {
            replyToMessageSafely(ch, "Unknown type ${contents[1]}... Format : `cd.lo -b/-u/-a/-m`\n" +
                    "\n" +
                    "-b : Bug fix\n" +
                    "-u : Update\n" +
                    "-a : API Update\n" +
                    "-m : Maintenance\n", loader.message) { a -> a }

            return
        }

        registerConfirmButtons(ch.sendMessage("Are you sure that you want to turn off the bot?"), 0).queue { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, ch.id, LangID.EN) {
                val self = ch.jda.selfUser.asMention
                val channel = loader.guild.getGuildChannelById(CardData.statusChannel)

                if (channel != null && channel is GuildMessageChannel) {
                    val message = "%s will be temporarily offline due to %s".format(self, reason)

                    channel.sendMessage(message).queue { _ ->
                        StaticStore.safeClose = true

                        CardBot.saveCardData()

                        client.shutdown()

                        System.exit(0)
                    }
                }
            })
        }
    }
}