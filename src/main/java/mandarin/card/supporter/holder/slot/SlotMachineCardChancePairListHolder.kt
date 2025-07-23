package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CardChancePairAmountHolder
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardGroupData
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotMachine
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit

class SlotMachineCardChancePairListHolder(
    author: Message,
    userID: String,
    channelID: String,
    message: Message,
    private val slotMachine: SlotMachine,
    private val content: SlotCardContent,
    private val cardChancePairList: CardChancePairList,
    private val new: Boolean
) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Slot machine manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of rolled cards")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("amount", "Rolled Cards Amount")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardChancePairAmountHolder(authorMessage, userID, channelID, message, cardChancePairList))
            }
            "pair" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()

                val pairList = cardChancePairList.pairs[index]

                connectTo(event, SlotMachineCardChancePairHolder(authorMessage, userID, channelID, message, slotMachine, cardChancePairList, pairList, false))
            }
            "add" -> {
                val pairList = CardChancePair(0.0, CardGroupData(ArrayList(), ArrayList()))

                connectTo(event, SlotMachineCardChancePairHolder(authorMessage, userID, channelID, message, slotMachine, cardChancePairList, pairList, true))
            }
            "create" -> {
                cardChancePairList.validateChance()

                content.cardChancePairLists.add(cardChancePairList)

                if (slotMachine in CardData.slotMachines) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card/chance pair list to the reward! Check result above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(event, "Are you sure you want to cancel creating card/chance pair list? This cannot be undone")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        goBack(e)
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    cardChancePairList.validateChance()

                    if (slotMachine in CardData.slotMachines) {
                        CardBot.saveCardData()
                    }

                    goBack(event)
                }
            }
            "delete" -> {
                registerPopUp(event, "Are you sure you want to delete card/chance pair list? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    content.cardChancePairLists.remove(cardChancePairList)

                    if (slotMachine in CardData.slotMachines) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully delete card/chance pair list! Check result above")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, CommonStatic.Lang.Locale.EN))
            }
            "cancel" -> {
                registerPopUp(event, "Are you sure you want to cancel creation of slot machine? This cannot be undone")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    e.deferReply()
                        .setContent("Canceled creation of slot machine")
                        .setEphemeral(true)
                        .queue()

                    goBackTo(SlotMachineListHolder::class.java)
                }, CommonStatic.Lang.Locale.EN))
            }
        }
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder()

        builder.append("# ").append(slotMachine.name).append("\n")
            .append("## Slot Machine Reward Create Section\n")
            .append("In this section, you can adjust how bot will roll the card rewards.")
            .append(" Card chance pair list is group of paris of card and chance with specific amount.")
            .append(" Define the amount that will be rolled for this pool.")
            .append(" Then you can add card/chance pair, or modify already-added one\n")
            .append("### Card Pool Info\n")
            .append("- ").append(cardChancePairList.amount).append(" ")

        if (cardChancePairList.amount >= 2) {
            builder.append("Cards\n")
        } else {
            builder.append("Card\n")
        }

        cardChancePairList.pairs.forEachIndexed { index, p ->
            builder.append("  - ").append(CardData.df.format(p.chance)).append("% : ").append(p.cardGroup.getName())

            if (index < cardChancePairList.pairs.size - 1) {
                builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(
            ActionRow.of(
            Button.secondary("amount", "Adjust Card Amount")
        ))

        val options = ArrayList<SelectOption>()

        if (cardChancePairList.pairs.isNotEmpty()) {
            for (i in cardChancePairList.pairs.indices) {
                options.add(SelectOption.of((i + 1).toString(), i.toString()))
            }
        } else {
            options.add(SelectOption.of("A", "A"))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("pair")
                    .addOptions(options)
                    .setPlaceholder("Select card/chance pair to adjust it")
                    .setDisabled(cardChancePairList.pairs.isEmpty())
                    .build()
            ))

        result.add(
            ActionRow.of(
                Button.secondary("add", "Add Card/Chance Pair")
                    .withEmoji(Emoji.fromUnicode("➕"))
                    .withDisabled(cardChancePairList.pairs.size >= StringSelectMenu.OPTIONS_MAX_AMOUNT)
            )
        )

        val buttons = ArrayList<Button>()

        if (new) {
            buttons.add(Button.success("create", "Create").withEmoji(EmojiStore.CHECK).withDisabled(cardChancePairList.amount == 0 || cardChancePairList.pairs.isEmpty()))
            buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))
        } else {
            buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK).withDisabled(cardChancePairList.amount == 0 || cardChancePairList.pairs.isEmpty()))
            buttons.add(Button.danger("delete", "Delete").withEmoji(EmojiStore.CROSS))
        }

        if (slotMachine !in CardData.slotMachines) {
            buttons.add(Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS))
        }

        result.add(ActionRow.of(buttons))

        return result
    }
}