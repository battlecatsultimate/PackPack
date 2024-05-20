package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.auction.AuctionCreateHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class CreateAuction : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member
        val ch = loader.channel

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val placeChannel = CardData.findPossibleAuctionPlace()

        if (placeChannel == -1L) {
            replyToMessageSafely(ch, "There's no possible place to open more auction! Please assign more auction place channel, or wait for other auctions to end", loader.message) { a -> a }

            return
        }

        val segments = loader.content.trim().split(Regex(" "))

        val anonymous = "-p" !in segments

        val possibleMember = segments.find { s -> StaticStore.isNumeric(s) || s.matches(Regex("<@\\d+>")) }

        println(possibleMember)

        val targetMember: Member? = if (possibleMember != null)
            findMember(loader.guild, possibleMember)
        else
            null

        if (possibleMember != null && targetMember == null) {
            replyToMessageSafely(ch, "Failed to retrieve member data from offered ID/Mention. Maybe ID is invalid or user left the server?", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "## Preparation of auction creation\n" +
                "\n" +
                "Auction author : ${targetMember?.asMention ?: "System"}\n" +
                "Auction Place : <#$placeChannel>\n" +
                "Anonymous? : ${if (anonymous) "True" else "False"}\n" +
                "\n" +
                "Selected Card : __Need Selection__\n" +
                "End Time : __Need to be decided__\n" +
                "Initial Price : __Need to be decided__",
            loader.message, { a -> a.setComponents(getComponents(anonymous)) }) { msg ->
            StaticStore.putHolder(m.id, AuctionCreateHolder(loader.message, ch.id, msg, targetMember?.idLong ?: -1L, placeChannel, anonymous))
        }
    }

    private fun findMember(g: Guild, segment: String) : Member? {
        val actualId = if (StaticStore.isNumeric(segment)) {
            segment
        } else {
            val filteredId = segment.replace("<@", "").replace(">", "")

            if (!StaticStore.isNumeric(filteredId)) {
                return null
            }

            filteredId
        }

        val memberReference = AtomicReference<Member?>(null)
        val countDownLatch = CountDownLatch(1)

        g.retrieveMember(UserSnowflake.fromId(actualId)).queue({ m ->
            memberReference.set(m)

            countDownLatch.countDown()
        }) { _ ->
            countDownLatch.countDown()
        }

        countDownLatch.await()

        return memberReference.get()
    }

    private fun getComponents(anonymous: Boolean) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.secondary("card", "Card").withEmoji(EmojiStore.ABILITY["CARD"]),
            Button.secondary("duration", "Duration").withEmoji(Emoji.fromUnicode("⏰")),
            Button.secondary("price", "Initial Price").withEmoji(EmojiStore.ABILITY["CF"]),
            Button.secondary("bid", "Minimum Bid").withEmoji(Emoji.fromUnicode("\uD83D\uDCB5"))
        ))

        result.add(ActionRow.of(
            Button.secondary("anonymous", "Anonymous?").withEmoji(if (anonymous) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
        ))

        result.add(ActionRow.of(
            Button.secondary("autoClose", "Auto Close?").withEmoji(EmojiStore.SWITCHOFF),
            Button.secondary("closeTime", "Auto Close Time").withEmoji(Emoji.fromUnicode("⏱️")).asDisabled()
        ))

        result.add(
            ActionRow.of(
                Button.success("start", "Start Auction").asDisabled(),
                Button.danger("cancel", "Cancel")
            )
        )

        return result
    }
}