package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.slot.SlotCardContent
import mandarin.card.supporter.slot.SlotCurrencyContent
import mandarin.card.supporter.slot.SlotMachine
import mandarin.card.supporter.slot.SlotPlaceHolderContent
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import java.util.concurrent.TimeUnit

class SlotMachineRewardTypeHolder(author: Message, userID: String, channelID: String, message: Message, private val slotMachine: SlotMachine) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "card" -> {
                connectTo(event, SlotMachineCardRewardHolder(authorMessage, userID, channelID, message, slotMachine, SlotCardContent("", 0L), true))
            }
            "currency" -> {
                connectTo(event, SlotMachineCurrencyRewardHolder(authorMessage, userID, channelID, message, slotMachine, SlotCurrencyContent("", 0L), true))
            }
            "placeHolder" -> {
                connectTo(event, SlotMachinePlaceHolderRewardHolder(authorMessage, userID, channelID, message, slotMachine, SlotPlaceHolderContent("", 0L), true))
            }
            "back" -> {
                goBack(event)
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

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "# ${slotMachine.name}\n" +
                "## Slot Machine Reward Type Section\n" +
                "In this section, you can decide which reward type this reward will have." +
                " There are three reward types : Currency, Card, and Place Holder." +
                " Currency mode will follow this slot machine's entry fee type." +
                " For example, if users have to put ${EmojiStore.ABILITY["CF"]?.formatted} Cat Foods as entry fee, reward will give ${EmojiStore.ABILITY["CF"]?.formatted} Cat Foods as well." +
                " Card mode will allow managers to create its own some form of card pack." +
                " If user got this reward, bot will roll RNG pool of it, and give cards to them." +
                " Lastly, Place Holder mode is only for holding emoji. It doesn't give anything to users." +
                " This can be used if you want to add only emojis into the slot machine\n" +
                "\n" +
                "**__Once you decide the reward type, you can't change it to other type__**"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            Button.secondary("card", "Card").withEmoji(EmojiStore.ABILITY["CARD"]),
            Button.secondary("currency", "Currency").withEmoji(EmojiStore.ABILITY["CF"]),
            Button.secondary("placeHolder", "Place Holder").withEmoji(EmojiStore.UNKNOWN)
        ))

        if (slotMachine !in CardData.slotMachines) {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
                Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
            ))
        } else {
            result.add(ActionRow.of(
                Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
            ))
        }

        return result
    }
}