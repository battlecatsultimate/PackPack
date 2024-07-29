package mandarin.card.commands


import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import kotlin.system.exitProcess

class LogOut : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
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

        registerConfirmButtons(ch.sendMessage("Are you sure that you want to turn off the bot?"), CommonStatic.Lang.Locale.EN).queue { msg ->
            StaticStore.logger.uploadLog("Logging out : ${loader.user.asMention}")

            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, ch.id, CommonStatic.Lang.Locale.EN) {
                val self = ch.jda.selfUser.asMention
                val channel = if (CardBot.test) {
                    null
                } else {
                    loader.guild.getGuildChannelById(CardData.statusChannel)
                }

                if (channel != null && channel is GuildMessageChannel) {
                    val message = "%s will be temporarily offline due to %s".format(self, reason)

                    channel.sendMessage(message).queue { _ ->
                        replyToMessageSafely(ch, "Good bye!", loader.message, { a -> a }) { _ ->
                            StaticStore.safeClose = true

                            CardBot.saveCardData()

                            client.shutdown()

                            exitProcess(0)
                        }
                    }
                } else {
                    replyToMessageSafely(ch, "Good bye!", loader.message, { a -> a }) { _ ->
                        StaticStore.safeClose = true

                        CardBot.saveCardData()

                        client.shutdown()

                        exitProcess(0)
                    }
                }
            })
        }
    }
}