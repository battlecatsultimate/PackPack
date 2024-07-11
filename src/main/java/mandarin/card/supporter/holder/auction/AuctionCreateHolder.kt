package mandarin.card.supporter.holder.auction

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.AuctionSession
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.auction.AuctionAutoCloseHolder
import mandarin.card.supporter.holder.modal.auction.AuctionEndTimeHolder
import mandarin.card.supporter.holder.modal.auction.AuctionInitialPriceHolder
import mandarin.card.supporter.holder.modal.auction.AuctionMinimumBidHolder
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class AuctionCreateHolder(author: Message, channelID: String, message: Message, private val authorID: Long, private val auctionPlace: Long, private var anonymous: Boolean) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var selectedCard: Card? = null
    private var amount = -1
    private var endTime = -1L
    private var initialPrice = -1L
    private var minimumBid = CardData.MINIMUM_BID
    private var autoClose = false
    private var autoCloseTime = CardData.AUTO_CLOSE_TIME

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "card" -> {
                connectTo(event, AuctionCardSelectHolder(authorMessage, channelID, message, if (authorID == -1L) null else Inventory.getInventory(authorID)) { card, count ->
                    selectedCard = card
                    amount = count
                })
            }
            "duration" -> {
                val input = TextInput.create("time", "End Time (UTC Time Zone)", TextInputStyle.SHORT)
                    .setPlaceholder("Example : 2024-5-30-18-40-00")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("endTime", "Decide Auction End Time")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionEndTimeHolder(authorMessage, channelID, message) { time ->
                    endTime = time

                    message.editMessage(getContent())
                        .setComponents(getComponent())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                })
            }
            "price" -> {
                val input = TextInput.create("price", "Initial Price", TextInputStyle.SHORT)
                    .setPlaceholder("Example : 1000000, 100k, 2m")
                    .build()

                val modal = Modal.create("price", "Decide Initial Price")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionInitialPriceHolder(authorMessage, channelID, message) { price ->
                    initialPrice = price

                    message.editMessage(getContent())
                        .setComponents(getComponent())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                })
            }
            "bid" -> {
                val input = TextInput.create("bid", "Minimum Bid Increase", TextInputStyle.SHORT)
                    .setPlaceholder("Example : 1000000, 100k, 2m")
                    .build()

                val modal = Modal.create("minimumBid", "Decide Minimum Bid Increase")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionMinimumBidHolder(authorMessage, channelID, message) { bid ->
                    minimumBid = bid

                    message.editMessage(getContent())
                        .setComponents(getComponent())
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                })
            }
            "anonymous" -> {
                anonymous = !anonymous

                event.deferEdit()
                    .setContent(getContent())
                    .setComponents(getComponent())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "autoClose" -> {
                autoClose = !autoClose

                event.deferEdit()
                    .setContent(getContent())
                    .setComponents(getComponent())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "closeTime" -> {
                val input = TextInput.create("time", "Time", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("i.e. 43200 = 12 hours, 3d6h30m - 3 days 6 hours 30 minutes")
                    .build()

                val modal = Modal.create("autoClose", "Auto Close Time")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionAutoCloseHolder(authorMessage, channelID, message) { time ->
                    autoCloseTime = time
                })
            }
            "start" -> {
                val card = selectedCard ?: return

                val auctionSession = AuctionSession(CardData.auctionSessionNumber++, auctionPlace, authorID, card, amount, endTime, anonymous, initialPrice, minimumBid, autoClose, autoCloseTime)

                auctionSession.queueSession(authorMessage.jda)

                TransactionLogger.logAuctionCreate(authorMessage.author.idLong, auctionSession)

                CardData.auctionSessions.add(auctionSession)

                if (authorID != -1L) {
                    val inventory = Inventory.getInventory(authorID)

                    inventory.cards[card] = (inventory.cards[card] ?: 0) - amount
                    inventory.auctionQueued[card] = (inventory.auctionQueued[card] ?: 0) + amount

                    if ((inventory.cards[card] ?: 0) < 0) {
                        inventory.cards.remove(card)
                    }
                }

                CardBot.saveCardData()

                event.deferEdit()
                    .setContent("Successfully started auction in <#${auctionPlace}> [$auctionPlace]!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true

                expire()
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled creation of the auction")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true
            }
        }
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponent())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onBack(child: Holder) {
        message.editMessage(getContent())
            .setComponents(getComponent())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder("## Preparation of auction creation\n\nAuction author : ")

        if (authorID == -1L) {
            builder.append("System")
        } else {
            builder.append("<@").append(authorID).append(">")
        }

        builder.append("\nAuction Place : <#").append(auctionPlace).append(">")
        builder.append("\nAnonymous? : ")

        if (anonymous)
            builder.append("True")
        else
            builder.append("False")

        builder.append("\n\nSelected Card : ")

        if (selectedCard == null) {
            builder.append("__Need Selection__")
        } else {
            builder.append(selectedCard?.cardInfo())

            if (amount >= 2L) {
                builder.append(" x").append(amount)
            }
        }

        builder.append("\nEnd Time : ")

        if (endTime == -1L) {
            builder.append("__Need to be decided__")
        } else {
            builder.append("<t:")
                .append(endTime)
                .append(":f> (<t:")
                .append(endTime)
                .append(":R>)")
        }

        builder.append("\nInitial Price : ")

        if (initialPrice == -1L) {
            builder.append("__Need to be decided__")
        } else {
            builder.append(EmojiStore.ABILITY["CF"]?.formatted)
                .append(" ")
                .append(initialPrice)
        }

        builder.append("\nMinimum Bid Increase : ")
            .append(EmojiStore.ABILITY["CF"]?.formatted)
            .append(" ")
            .append(minimumBid)

        builder.append("\nAuto Close? : ")

        if (autoClose) {
            builder.append("True\nAuto Close Time : ")
                .append(CardData.convertMillisecondsToText(autoCloseTime))
        } else {
            builder.append("False")
        }

        builder.append("\n\n### Auto Close?\nAuto close auction is an auction that bot will automatically close the auction if there's no bid for 12 hours. ")

        return builder.toString()
    }

    private fun getComponent() : List<LayoutComponent> {
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
            Button.secondary("autoClose", "Auto Close?").withEmoji(if (autoClose) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF),
            Button.secondary("closeTime", "Auto Close Time").withEmoji(Emoji.fromUnicode("⏱️")).withDisabled(!autoClose)
        ))

        result.add(
            ActionRow.of(
                Button.success("start", "Start Auction").withDisabled(selectedCard == null || amount == -1 || endTime == -1L || initialPrice == -1L || (autoClose && autoCloseTime == -1L)),
                Button.danger("cancel", "Cancel")
            )
        )

        return result
    }
}