package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class OptOut : Command(LangID.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        replyToMessageSafely(loader.channel,
            "Users have right to opt out from the bot collecting your data. " +
            "This command will allow you to opt out from services that are provided by the bot. " +
            "**__Keep in mind that once you opt out, you won't be able to opt in back unless you contact the developer of the bot, <@${StaticStore.MANDARIN_SMELL}>.__** \n\n" +
            "Below lists are wiped data upon opting out :\n\n" +
            "- Inventory\n" +
            "- Trading Session\n" +
            "- Auction\n" +
            "- Notification Preferences\n" +
            "- Any Logged Data in The Bot\n\n" +
            "[Privacy Policy](https://github.com/battlecatsultimate/PackPack/blob/main/Privacy%20Policy%20CardDealer.md) is here\n" +
            "## Are you really sure you want to opt out? Once this process is done, you can't undo it",
            loader.message,
            { a -> registerConfirmButtons(a, LangID.EN) }
        ) { msg ->
            StaticStore.putHolder(loader.user.id, ConfirmButtonHolder(loader.message, msg, loader.channel.id, LangID.EN) {
                replyToMessageSafely(loader.channel, "This is secondary confirmation message\n# Are you sure you want to opt out?", loader.message, { a -> registerConfirmButtons(a, LangID.EN) }) { message ->
                    StaticStore.putHolder(loader.user.id, ConfirmButtonHolder(loader.message, message, loader.channel.id, LangID.EN) {
                        val id = loader.user.idLong

                        CardData.inventories.remove(id)
                        CardData.sessions.removeIf { session ->
                            return@removeIf id in session.member
                        }
                        CardData.auctionSessions.removeIf { session ->
                            return@removeIf session.author == id
                        }
                        CardData.notifierGroup.remove(id)

                        val logs = LogSession.gatherPreviousSessions(CardData.getUnixEpochTime(), 0)

                        logs.forEach { session ->
                            session.optOut(id)

                            session.saveSessionAsFile()
                        }

                        CardData.optOut.add(id)

                        CardBot.saveCardData()

                        replyToMessageSafely(loader.channel, "Opt out is successfully done! Thanks for using service of CardDealer", loader.message) { a -> a }
                    })
                }
            })
        }
    }
}