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

class ECCSelectHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "validate" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val validationWay = Inventory.ECCValidationWay.valueOf(event.values.first())

                connectTo(event, ECCValidationHolder(authorMessage, userID, channelID, message, validationWay))
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
        return "Please select how you will validate ECC\n\n" +
                "- 15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card\n" +
                "- 2 Unique T4 Cards\n" +
                "- 3 Same T4 Cards\n" +
                "- Legendary Collector\n\n" +
                "**Keep in mind that once you obtain ECC, you can't retrieve cards that you have spent**"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        Inventory.ECCValidationWay.entries.forEach { v ->
            if (v == Inventory.ECCValidationWay.NONE || v == Inventory.ECCValidationWay.CUSTOM_ROLE || v == Inventory.ECCValidationWay.MANUAL)
                return@forEach

            val doable = if (Inventory.checkECCDoable(v, inventory).isBlank()) {
                "Can be validated"
            } else {
                "Cannot be validated"
            }

            val label = when (v) {
                Inventory.ECCValidationWay.LEGENDARY_COLLECTOR -> "Legendary Collector"
                Inventory.ECCValidationWay.SEASONAL_15_COLLAB_12_T4 -> "15 Unique Seasonal Cards + 12 Unique Collaboration Cards + 1 T4 Card"
                Inventory.ECCValidationWay.T4_2 -> "2 Unique T4 Cards"
                Inventory.ECCValidationWay.SAME_T4_3 -> "3 Same T4 Cards"
                else -> throw IllegalStateException("E/ECCSelectHolder::getComponents - Unhandled validation way : $v")
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