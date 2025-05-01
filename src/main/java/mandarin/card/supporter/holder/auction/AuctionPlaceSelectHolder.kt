package mandarin.card.supporter.holder.auction

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.auction.AuctionBidHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class AuctionPlaceSelectHolder(author: Message, userID: String, channelID: String, message: Message, private val guild: Guild) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "auction" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val value = event.values[0].toLong()

                val auctionSession = CardData.auctionSessions.find { s -> s.channel == value } ?: return

                if ((auctionSession.bidData[event.user.idLong] ?: 0) == -1L) {
                    event.deferReply()
                        .setContent("It seems that you have left the server while participating this auction. So you aren't allowed to bid this auction")
                        .setEphemeral(true)
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                if (auctionSession.bidData.containsKey(event.user.idLong) && auctionSession.currentBid == (auctionSession.bidData[event.user.idLong] ?: 0)) {
                    event.deferReply()
                        .setContent("You can't bid again while no one has bid! Wait for other user's bid")
                        .setEphemeral(true)
                        .mentionRepliedUser(false)
                        .queue()

                    return
                }

                val textInput = TextInput.create("bid", "Bid Cat Foods (Minimum ${auctionSession.currentBid + auctionSession.minimumBid})", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setValue(null)
                    .setPlaceholder("i.e. 3000, 2k, 1m")
                    .build()

                val modal = Modal.create("bid", "Bid The Auction")
                    .addActionRow(textInput)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionBidHolder(authorMessage, userID, channelID, message, auctionSession) { bid ->
                    registerPopUp(
                        message,
                        "Are you sure you want to bid ${EmojiStore.ABILITY["CF"]?.formatted} $bid to Auction #${auctionSession.id} <#${auctionSession.channel}>? You won't be able to use bid cat foods in other place until you cancel the bid",
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        auctionSession.bid(event.jda.shardManager, authorMessage.author.idLong, bid)

                        e.deferEdit()
                            .setContent("Successfully bid ${EmojiStore.ABILITY["CF"]?.formatted} $bid to Auction #${auctionSession.id} <#${auctionSession.channel}>!")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()

                        end(true)
                    }, CommonStatic.Lang.Locale.EN))
                })
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled bid")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        message.editMessage(
            "Please select auction place where you want to bid\n" +
                    "\n" +
                    "You can bid up to ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood}")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onExpire() {
        message.editMessage("Auction creation expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent("Please select auction place where you want to bid\n" +
                    "\n" +
                    "You can bid up to ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood}")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (channel in CardData.auctionSessions.filter { s -> s.opened }.map { s -> s.channel }) {
            val ch = guild.getGuildChannelById(channel) ?: continue

            options.add(SelectOption.of(ch.name, ch.id))
        }

        result.add(ActionRow.of(StringSelectMenu.create("auction").addOptions(options).setDefaultOptions().build()))
        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}