package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.auction.AuctionPlaceSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Bid : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user
        val ch = loader.channel

        val inventory = Inventory.getInventory(u.idLong)
        val segments = loader.content.split(Regex(" "), 3)

        val g = u.jda.getGuildById(CardData.guild) ?: return

        if (ch is PrivateChannel) {
            if (segments.size < 3) {
                if (CardData.auctionSessions.filter { s -> s.opened}.isEmpty()) {
                    replyToMessageSafely(ch, "Currently there's no auction going on!", loader.message) { a -> a }

                    return
                }

                replyToMessageSafely(ch, "Please select auction place where you want to bid\n\nYou can bid up to ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood}", loader.message, { a -> a.setComponents(getComponents(g)) }) { msg ->
                    StaticStore.putHolder(u.id, AuctionPlaceSelectHolder(loader.message, u.id, ch.id, msg, g))
                }
            } else {
                val id = getChannelID(segments[1])

                if (id == -1L) {
                    replyToMessageSafely(ch, "It seems you have passed invalid channel format. Channel ID must be passed as raw number or mention", loader.message) { a -> a }

                    return
                }

                val auctionSession = CardData.auctionSessions.find { s -> s.channel == id }

                if (auctionSession == null) {
                    replyToMessageSafely(ch, "Failed to find auction from provided channel. Maybe incorrect ID or auction has been closed already?", loader.message) { a -> a }

                    return
                }

                if (auctionSession.author == u.idLong) {
                    replyToMessageSafely(ch, "You can't bid your auction!", loader.message) { a -> a }

                    return
                }

                if (!auctionSession.opened) {
                    replyToMessageSafely(ch, "This auction is closed already, you can't participate this auction anymore", loader.message) { a -> a }

                    return
                }

                if ((auctionSession.bidData[u.idLong] ?: 0) == -1L) {
                    replyToMessageSafely(ch, "It seems that you have left the server while participating this auction. So you aren't allowed to bid this auction", loader.message) { a -> a }

                    return
                }

                val catFoods = getCatFoods(segments[2])

                if (catFoods == null) {
                    replyToMessageSafely(ch, "Please pass numeric value!", loader.message) { a -> a }

                    return
                }

                if (catFoods < 0) {
                    replyToMessageSafely(ch, "You have to bid positive numbers! ...", loader.message) { a -> a }

                    return
                }

                if (catFoods < auctionSession.currentBid + auctionSession.minimumBid) {
                    if (catFoods <= auctionSession.currentBid) {
                        replyToMessageSafely(ch, "You have to bid over ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.currentBid + auctionSession.minimumBid}!", loader.message) { a -> a }
                    } else {
                        replyToMessageSafely(ch, "You have to bid over ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.minimumBid} than current bid!", loader.message) { a -> a }
                    }

                    return
                }

                if (catFoods > inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)) {
                    replyToMessageSafely(ch, "You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)}. You can't bid more than what you have!", loader.message) { a -> a }

                    return
                }

                replyToMessageSafely(ch, "Are you sure you want to bid ${EmojiStore.ABILITY["CF"]?.formatted} $catFoods? You won't be able to use bid cat foods in other place until you cancel the bid", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
                    StaticStore.putHolder(u.id, ConfirmButtonHolder(loader.message, u.id, ch.id, msg, CommonStatic.Lang.Locale.EN) {
                        auctionSession.bid(loader.client.shardManager, u.idLong, catFoods)

                        replyToMessageSafely(ch, "Successfully posted to the bid $catFoods ${EmojiStore.ABILITY["CF"]?.formatted} to auction #${auctionSession.id}!", loader.message) { a -> a }
                    })
                }
            }
        } else {
            if (ch.idLong !in CardData.auctionPlaces)
                return

            val auctionSession = CardData.auctionSessions.find { s -> s.channel == ch.idLong }

            if (auctionSession == null) {
                return
            }

            if (auctionSession.author == u.idLong) {
                if (!auctionSession.anonymous) {
                    replyToMessageSafely(ch, "You can't bid your auction!", loader.message) { a -> a }
                }

                return
            }

            if (!auctionSession.opened) {
                return
            }

            if (auctionSession.anonymous) {
                loader.message.delete().queue {
                    replyToMessageSafely(ch, "This auction is anonymous auction! Users shall not see who bid this auction. Please call this command via DM", loader.message) { a -> a }
                }

                return
            }

            if ((auctionSession.bidData[u.idLong] ?: 0) == -1L) {
                replyToMessageSafely(ch, "It seems that you have left the server while participating this auction. So you aren't allowed to bid this auction", loader.message) { a -> a }

                return
            }

            if (segments.size < 2) {
                replyToMessageSafely(ch, "Please provide how much cat food ${EmojiStore.ABILITY["CF"]?.formatted} you will bid! Format is `${CardBot.globalPrefix}bid [Cat Food]`", loader.message) { a -> a }

                return
            }

            val catFoods = getCatFoods(segments[1])

            if (catFoods == null) {
                replyToMessageSafely(ch, "Please pass numeric value!", loader.message) { a -> a }

                return
            }

            if (catFoods < 0) {
                replyToMessageSafely(ch, "You have to bid positive numbers! ...", loader.message) { a -> a }

                return
            }

            if (catFoods < auctionSession.currentBid + auctionSession.minimumBid) {
                if (catFoods <= auctionSession.currentBid) {
                    replyToMessageSafely(ch, "You have to bid over ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.currentBid + auctionSession.minimumBid}!", loader.message) { a -> a }
                } else {
                    replyToMessageSafely(ch, "You have to bid over ${EmojiStore.ABILITY["CF"]?.formatted} ${auctionSession.minimumBid} than current bid!", loader.message) { a -> a }
                }

                return
            }

            if (catFoods > inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)) {
                replyToMessageSafely(ch, "You currently have ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood + (auctionSession.bidData[u.idLong] ?: 0)}. You can't bid more than what you have!", loader.message) { a -> a }

                return
            }

            replyToMessageSafely(ch, "Are you sure you want to bid ${EmojiStore.ABILITY["CF"]?.formatted} $catFoods? You won't be able to use bid cat foods in other place until you cancel the bid", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
                StaticStore.putHolder(u.id, ConfirmButtonHolder(loader.message, u.id, ch.id, msg, CommonStatic.Lang.Locale.EN) {
                    auctionSession.bid(loader.client.shardManager, u.idLong, catFoods)

                    replyToMessageSafely(ch, "Successfully posted to the bid $catFoods ${EmojiStore.ABILITY["CF"]?.formatted} to auction #${auctionSession.id}!", loader.message) { a -> a }
                })
            }
        }
    }

    private fun getCatFoods(value: String) : Long? {
        return if (StaticStore.isNumeric(value)) {
            StaticStore.safeParseLong(value)
        } else {
            val suffix = when {
                value.endsWith("k") -> "k"
                value.endsWith("m") -> "m"
                value.endsWith("b") -> "b"
                else -> ""
            }

            if (suffix.isEmpty()) {
                return null
            }

            val filteredValue = value.replace(suffix, "")

            if (!StaticStore.isNumeric(filteredValue)) {
                return null
            }

            val multiplier = when(suffix) {
                "k" -> 1000
                "m" -> 1000000
                "b" -> 1000000000
                else -> 1
            }

            (StaticStore.safeParseDouble(filteredValue) * multiplier).toLong()
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

    private fun getComponents(g: Guild) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (channel in CardData.auctionSessions.filter { s -> s.opened }.map { s -> s.channel }) {
            val ch = g.getGuildChannelById(channel) ?: continue

            options.add(SelectOption.of(ch.name, ch.id))
        }

        result.add(ActionRow.of(StringSelectMenu.create("auction").addOptions(options).build()))
        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}