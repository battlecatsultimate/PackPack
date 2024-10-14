package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.log.LogSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class OptOut : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        replyToMessageSafely(loader.channel,
            "Users have right to opt out from the bot collecting your data. " +
            "This command will allow you to opt out from services that are provided by the bot. " +
            "**__Keep in mind that once you opt out, you won't be able to opt in back unless you contact the developer of the bot, <@${StaticStore.MANDARIN_SMELL}>.__** \n\n" +
            "Below lists are wiped data upon opting out :\n\n" +
            "- Inventory\n" +
            "- Your Registered Skins" +
            "- Trading Session\n" +
            "- Auction\n" +
            "- Notification Preferences\n" +
            "- Any Logged Data in The Bot\n\n" +
            "[Privacy Policy](https://github.com/battlecatsultimate/PackPack/blob/main/Privacy%20Policy%20CardDealer.md) is here\n" +
            "## Are you really sure you want to opt out? Once this process is done, you can't undo it",
            loader.message,
            { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }
        ) { msg ->
            StaticStore.putHolder(loader.user.id, ConfirmButtonHolder(loader.message, loader.user.id, loader.channel.id, msg, CommonStatic.Lang.Locale.EN) {
                replyToMessageSafely(loader.channel, "This is secondary confirmation message\n# Are you sure you want to opt out?", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { message ->
                    StaticStore.putHolder(loader.user.id, ConfirmButtonHolder(loader.message, loader.user.id, loader.channel.id, message, CommonStatic.Lang.Locale.EN) {
                        val id = loader.user.idLong

                        CardData.inventories.remove(id)

                        CardData.skins.removeIf { skin ->
                            if (skin.creator == id) {
                                CardData.inventories.forEach { (_, inventory) ->
                                    inventory.skins.remove(skin)
                                    inventory.equippedSkins.entries.removeIf { (_, s) ->
                                        return@removeIf s == skin
                                    }
                                }
                            }

                            return@removeIf skin.creator == id
                        }

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