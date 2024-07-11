package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class ForceCancelBid : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid")) {
            if (CardData.isManager(m)) {
                replyToMessageSafely(ch, "This command currently can be run only by <@${StaticStore.MANDARIN_SMELL}> or <@${ServerData.get("gid")}>!", loader.message) { a -> a }
            }

            return
        }

        val segments = loader.content.split(" ")

        if (segments.size < 2) {
            replyToMessageSafely(ch, "You have to provide user ID at least! Here are possible formats :\n" +
                    "\n" +
                    "`${CardBot.globalPrefix}forcecancelbid [Auction Place] [User] <-r Reason>` (When used outside of auction place itself)\n" +
                    "`${CardBot.globalPrefix}forcecancelbid [User] <-r Reason>` (When used inside of auction place\n" +
                    "\n" +
                    "Both auction place and user allows direct mention", loader.message) { a -> a }

            return
        }

        val auctionSession = if (segments.size >= 3) {
            CardData.auctionSessions.find { s -> s.channel == getChannelID(segments[1]) }
        } else {
            CardData.auctionSessions.find { s -> s.channel == ch.idLong }
        }

        if (auctionSession == null) {
            if (segments.size >= 3) {
                replyToMessageSafely(ch, "Failed to find auction channel from given information. Format is `${CardBot.globalPrefix}forcecancelbid [Auction Place] [User]`", loader.message) { a -> a }
            } else {
                replyToMessageSafely(ch, "Failed to find auction in this channel. Maybe this isn't auction place, or auction has been closed?\n" +
                        "If you want to call this command outside of auction place, please follow format below : \n" +
                        "\n" +
                        "`${CardBot.globalPrefix}forcecancelbid [Auction Place] [User]`", loader.message) { a -> a }
            }

            return
        }

        val user = if (segments.size >= 3) {
            getUserID(segments[2])
        } else {
            getUserID(segments[1])
        }

        if (!auctionSession.bidData.containsKey(user)) {
            replyToMessageSafely(ch, "This user <@$user> [$user] hasn't bid to this auction!\n" +
                    "\n" +
                    "Auction info : Auction #${auctionSession.id} <#${auctionSession.channel}> [${auctionSession.channel}]", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Are you sure you want to cancel the bid of this user? User will get notification as well", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, ch.id, CommonStatic.Lang.Locale.EN) {
                val previousBid = auctionSession.bidData[user] ?: 0

                auctionSession.forceCancelBid(m.idLong, user)

                replyToMessageSafely(ch, "Successfully canceled the bid!", loader.message) { a -> a }

                m.jda.retrieveUserById(user).queue { u ->
                    u.openPrivateChannel().queue { pc ->
                        var notification = "Your bid has been canceled by manager\n" +
                                "\n" +
                                "### Auction Info\n" +
                                "Auction Place : Auction #${auctionSession.id} (<#${auctionSession.channel}> [${auctionSession.channel}]\n" +
                                "Your Previous Bid : ${EmojiStore.ABILITY["CF"]?.formatted} $previousBid"

                        val reason = getReason(loader.content)

                        if (reason.isNotEmpty()) {
                            notification += "\n\nReason : \n" +
                                    "\n" +
                                    reason
                        }

                        pc.sendMessage(notification).queue()
                    }
                }
            })
        }
    }

    private fun getChannelID(value: String) : Long {
        return if (StaticStore.isNumeric(value)) {
            StaticStore.safeParseLong(value)
        } else {
            val filteredValue = value.replace("<#", "").replace(">", "")

            if (!StaticStore.isNumeric(filteredValue)) {
                return -1L
            }

            StaticStore.safeParseLong(filteredValue)
        }
    }

    private fun getUserID(value: String) : Long {
        return if (StaticStore.isNumeric(value)) {
            StaticStore.safeParseLong(value)
        } else {
            val filteredValue = value.replace("<@", "").replace(">", "")

            if (!StaticStore.isNumeric(filteredValue)) {
                return -1L
            }

            StaticStore.safeParseLong(filteredValue)
        }
    }

    private fun getReason(content: String) : String {
        val builder = StringBuilder()

        val segments = content.split(" ")

        var append = false

        for (segment in segments) {
            if (segment == "-r" || segment == "-reason") {
                append = true

                continue
            }

            if (append) {
                builder.append(segment).append(" ")
            }
        }

        return builder.toString().trim()
    }
}