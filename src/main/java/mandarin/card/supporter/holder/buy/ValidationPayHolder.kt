package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import kotlin.math.min

class ValidationPayHolder(author: Message, userID: String, channelID: String, message: Message, private val cards: List<Card>, private val cardList: ArrayList<Card>, private val amount: Int) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private var page = 0

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "card" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val card = cards[event.values.first().toInt()]

                if (card in cardList) {
                    cardList.remove(card)

                    applyResult(event)
                } else {
                    if (cardList.size < amount) {
                        cardList.add(card)

                        applyResult(event)
                    } else {
                        event.deferReply()
                            .setContent("You already chose all required cards! If you want to pay with another card, please unselect first")
                            .setEphemeral(true)
                            .queue()
                    }
                }
            }
            "all" -> {
                cards.filter { c -> c !in cardList }.forEach { c ->
                    if (cardList.size == amount)
                        return@forEach

                    cardList.add(c)
                }

                event.deferReply()
                    .setContent("Successfully selected all cards! Check the result above")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "dupe" -> {
                cards.filter { c -> c !in cardList }.filter { c -> (inventory.cards[c] ?: 0) >= 2 }.forEach { c ->
                    if (cardList.size == amount)
                        return@forEach

                    cardList.add(c)
                }

                event.deferReply()
                    .setContent("Successfully selected duplicated cards! Check the result above")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "prev" -> {
                page--

                applyResult(event)
            }
            "prev10" -> {
                page -= 10

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "next10" -> {
                page += 10

                applyResult(event)
            }
            "back" -> {
                goBack(event)
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel purchase? Your purchase progress will be lost")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferEdit()
                        .setContent("Purchase canceled")
                        .setComponents()
                        .setAllowedMentions(arrayListOf())
                        .mentionRepliedUser(false)
                        .queue()

                    end(true)
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {

    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Please choose the cards that you will pay. You can cancel choosing by selecting card again\n\n- **Card List**\n\n```md\n")

        if (cards.isEmpty()) {
            builder.append("No Cards")
        } else {
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(cards.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                builder.append(i + 1).append(". ").append(cards[i].simpleCardInfo())

                if ((inventory.cards[cards[i]] ?: 0) >= 2) {
                    builder.append(" x").append(inventory.cards[cards[i]] ?: 0)
                }

                if (cards[i] in cardList) {
                    builder.append(" [✅ Selected]")
                }

                builder.append("\n")
            }
        }

        builder.append("\nPage : ").append(page + 1).append("/").append(getTotalPage(cards.size)).append("\n")

        builder.append("```")

        if (cardList.size == amount) {
            builder.append("\n**You chose all required amount! Now you can go back**")
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val cardOption = ArrayList<SelectOption>()

        for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(cards.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
            cardOption.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()).withDescription(if (cards[i] in cardList) "Selected" else null))
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("card").addOptions(cardOption).setPlaceholder("Select cards to pay").build()
        ))

        if (cards.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val totalPage = getTotalPage(cards.size)

            val buttons = ArrayList<Button>()

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))
            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if(totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.secondary("all", "Select All").withDisabled(cardList.size == amount || cards.none { c -> c !in cardList }),
            Button.secondary("dupe", "Select Duplicated").withDisabled(cardList.size == amount || cards.filter { c -> c !in cardList}.none { c -> (inventory.cards[c] ?: 0) >= 2 } || !inventory.cards.keys.filter { c -> c in cards }.any { c -> (inventory.cards[c] ?: 0) >= 2 })
        ))

        result.add(ActionRow.of(
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}