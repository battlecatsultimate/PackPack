package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class AuctionPlace : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel

        val builder = StringBuilder("List of auction place\n\n")

        if (CardData.auctionPlaces.isEmpty()) {
            builder.append("- None")
        } else {
            for (id in CardData.auctionPlaces) {
                builder.append("- <#").append(id).append("> [")

                if (CardData.auctionSessions.any { s -> s.channel == id }) {
                    builder.append("Occupied")
                } else {
                    builder.append("Vacant")
                }

                builder.append("]\n")
            }
        }

        replyToMessageSafely(ch, builder.toString(), loader.message) { a -> a }
    }
}