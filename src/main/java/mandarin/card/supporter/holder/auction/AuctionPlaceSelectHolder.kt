package mandarin.card.supporter.holder.auction

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.modal.AuctionBidHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal

class AuctionPlaceSelectHolder(author: Message, channelID: String, private val message: Message, private val guild: Guild) : ComponentHolder(author, channelID, message) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "auction" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val value = event.values[0].toLong()

                val auctionSession = CardData.auctionSessions.find { s -> s.channel == value } ?: return

                val textInput = TextInput.create("bid", "Bid Cat Foods (Minimum ${auctionSession.currentBid + auctionSession.minimumBid})", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setValue(null)
                    .setPlaceholder("i.e. 3000, 2k, 1m")
                    .build()

                val modal = Modal.create("bid", "Bid The Auction")
                    .addActionRow(textInput)
                    .build()

                event.replyModal(modal).queue()

                connectTo(AuctionBidHolder(authorMessage, channelID, message, auctionSession) { bid ->
                    registerPopUp(message, "Are you sure you want to bid ${EmojiStore.ABILITY["CF"]?.formatted} $bid to Auction #${auctionSession.id} <#${auctionSession.channel}>? You won't be able to use bid cat foods in other place until you cancel the bid", LangID.EN)

                    StaticStore.putHolder(authorMessage.author.id, ConfirmPopUpHolder(authorMessage, message, channelID, { e ->
                        auctionSession.bid(authorMessage.author.idLong, bid)

                        e.deferEdit()
                            .setContent("Successfully bid ${EmojiStore.ABILITY["CF"]?.formatted} $bid to Auction #${auctionSession.id} <#${auctionSession.channel}>!")
                            .setComponents()
                            .setAllowedMentions(ArrayList())
                            .mentionRepliedUser(false)
                            .queue()

                        expired = true

                        expire()
                    }, { e ->
                        applyResult(e)

                        StaticStore.putHolder(authorMessage.author.id, this)
                    }, LangID.EN))
                })
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled bid")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                expired = true

                expire()
            }
        }
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack() {
        super.onBack()

        message.editMessage(
            "Please select auction place where you want to bid\n" +
                    "\n" +
                    "You can bid up to ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.actualCatFood}")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
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