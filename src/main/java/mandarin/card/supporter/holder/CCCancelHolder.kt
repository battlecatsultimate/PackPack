package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import kotlin.collections.component1
import kotlin.collections.component2

class CCCancelHolder(author: Message, userID: String, channelID: String, message: Message, private val cancelMode: CancelMode) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    enum class CancelMode {
        CC,
        ECC
    }

    val inventory = Inventory.getInventory(author.author.idLong)

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "confirm" -> {
                val guild = event.guild ?: return

                registerPopUp(event, "Are you really sure you want to cancel ${cancelMode.name}?")

                connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                    if (cancelMode == CancelMode.CC) {
                        TransactionLogger.logCCCancel(authorMessage.author.idLong, inventory)

                        inventory.cancelCC(guild, authorMessage.author.idLong)
                    } else {
                        TransactionLogger.logECCCancel(authorMessage.author.idLong, inventory)

                        inventory.cancelECC(guild, authorMessage.author.idLong)
                    }

                    CardBot.saveCardData()

                    e.deferEdit()
                        .setContent("Successfully cancelled ${cancelMode.name}! Check your cards to see if they are retrieved properly")
                        .setComponents()
                        .setAllowedMentions(arrayListOf())
                        .mentionRepliedUser(false)
                        .queue()

                    end(true)
                }, CommonStatic.Lang.Locale.EN))
            }
            "cancel" -> {
                event.deferEdit()
                    .setContent("Confirmation cancelled")
                    .setComponents()
                    .setAllowedMentions(arrayListOf())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    override fun onExpire() {

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

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            Button.success("confirm", "Confirm").withEmoji(EmojiStore.CHECK),
            Button.danger("cancel", "Cancel").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }

    private fun getContents() : String {
        return if (cancelMode == CancelMode.CC) {
            val cf = EmojiStore.ABILITY["CF"]?.formatted

            val builder = StringBuilder(
                "## Cancellation of CC\n" +
                        "This is message that informs you about details of cancellation of CC\n" +
                        "\n" +
                        "1. If there are cards or $cf that you have spent when validating CC, you will retrieve them\n" +
                        "2. If you also had ECC, ECC will be cancelled together because ECC requires users to have CC\n" +
                        "3. You can re-join CC or ECC again at any time"
            )

            val inventory = Inventory.getInventory(authorMessage.author.idLong)

            if (inventory.ccValidationWay != Inventory.CCValidationWay.LEGENDARY_COLLECTOR || (inventory.eccValidationWay != Inventory.ECCValidationWay.NONE && inventory.eccValidationWay != Inventory.ECCValidationWay.LEGENDARY_COLLECTOR)) {
                builder.append("\n\nBelow lists are what you will retrieve :\n")

                inventory.validationCards.entries.forEach { (card, pair) ->
                    builder.append("- ").append(card.simpleCardInfo())

                    if (pair.second >= 2) {
                        builder.append(" x").append(pair.second)
                    }

                    builder.append("\n")
                }

                val catFoods = when(inventory.ccValidationWay) {
                    Inventory.CCValidationWay.SEASONAL_15,
                    Inventory.CCValidationWay.COLLABORATION_12 -> 150000
                    Inventory.CCValidationWay.SEASONAL_15_COLLABORATION_12 -> 0
                    Inventory.CCValidationWay.T3_3 -> 200000
                    Inventory.CCValidationWay.LEGENDARY_COLLECTOR -> 0
                    Inventory.CCValidationWay.NONE -> 0
                }

                if (catFoods > 0) {
                    builder.append("- ").append(cf).append(" ").append(catFoods)
                }
            }

            builder.toString()
        } else {
            val builder = StringBuilder(
                "## Cancellation of ECC\n" +
                        "This is message that informs you about details of cancellation of ECC\n" +
                        "\n" +
                        "1. If there are cards that you have spent when validating ECC, you will retrieve them\n" +
                        "2. You can re-join ECC again at any time"
            )

            val inventory = Inventory.getInventory(authorMessage.author.idLong)

            if (inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC }.isNotEmpty()) {
                builder.append("\n\nBelow lists are what you will retrieve :\n")

                inventory.validationCards.filterValues { pair -> pair.first == Inventory.ShareStatus.ECC }.forEach { (card, pair) ->
                    builder.append("- ").append(card.simpleCardInfo())

                    if (pair.second >= 2) {
                        builder.append(" x").append(pair.second)
                    }

                    builder.append("\n")
                }
            }

            builder.toString()
        }
    }
}