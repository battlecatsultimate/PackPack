package mandarin.card.supporter.holder.buy

import common.CommonStatic
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu

class CCSelectHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "validate" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val validationWay = Inventory.CCValidationWay.valueOf(event.values.first())

                connectTo(event, CCValidationHolder(authorMessage, userID, channelID, message, validationWay))
            }
            "back" -> goBack(event)
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
        message.editMessage("Purchase expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
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

    private fun getContents() : String {
        val cf = EmojiStore.ABILITY["CF"]?.formatted

        return "Please select how you will validate CC\n\n" +
                "- 15 Unique Seasonal Cards + $cf 150k\n" +
                "- 12 Unique Collaboration Cards + $cf 150k\n" +
                "- 15 Unique Seasonal Cards + 12 Unique Collaboration Cards\n" +
                "- 3 Unique T3 Cards + $cf 200k\n" +
                "- Legendary Collector\n\n" +
                "**Keep in mind that once you obtain CC, you can't retrieve cards that you have spent**"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        Inventory.CCValidationWay.entries.forEach { v ->
            if (v == Inventory.CCValidationWay.NONE || v == Inventory.CCValidationWay.MANUAL)
                return@forEach

            val doable = if (Inventory.checkCCDoable(v, inventory).isBlank()) {
                "Can be validated"
            } else {
                "Cannot be validated"
            }

            val label = when (v) {
                Inventory.CCValidationWay.SEASONAL_15 -> "15 Unique Seasonal Cards + 150k Cat Foods"
                Inventory.CCValidationWay.COLLABORATION_12 -> "12 Unique Collaboration Cards + 150k Cat Foods"
                Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards"
                Inventory.CCValidationWay.T3_3 -> "3 Unique T3 Cards + 200k Cat Foods"
                Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                else -> throw IllegalStateException("E/CCSelectHolder::getComponents - Unhandled validation way : $v")
            }

            options.add(SelectOption.of(label, v.name).withDescription(doable))
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("validate").addOptions(options).setPlaceholder("Select Validation Way").build()
        ))

        result.add(ActionRow.of(
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}