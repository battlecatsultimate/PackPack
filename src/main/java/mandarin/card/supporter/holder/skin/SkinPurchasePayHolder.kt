package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.pack.CardCostPayHolder
import mandarin.card.supporter.pack.CardPayContainer
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
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
import java.util.concurrent.TimeUnit

class SkinPurchasePayHolder(author: Message, userID: String, channelID: String, message: Message, private val skin: Skin) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    val inventory = Inventory.getInventory(author.author.idLong)

    private val containers = Array(skin.cost.cardsCosts.size) {
        CardPayContainer(skin.cost.cardsCosts[it])
    }

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Skin manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "purchase" -> {
                var futureWarn = false
                val warn = if (!inventory.cards.containsKey(skin.card)) {
                    true
                } else {
                    val currentCards = inventory.cards[skin.card] ?: 0
                    val paidCards = containers.sumOf { container -> container.pickedCards.count { c -> c == skin.card } }

                    futureWarn = true
                    currentCards - paidCards <= 0
                }

                val purchaser = authorMessage.author.idLong

                if (warn) {
                    val content = if (futureWarn) {
                        "After payment, you won't own any of this card. Are you sure you want to purchase this skin? This cannot be undone"
                    } else {
                        "You don't own this card currently. Are you sure you want to purchase this skin? This cannot be undone"
                    }

                    registerPopUp(event, content)

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        skin.purchase(purchaser, inventory, containers)

                        if (inventory.skins.filter { s -> s.card == skin.card }.size == 1) {
                            inventory.equippedSkins[skin.card] = skin

                            e.deferReply()
                                .setContent("Successfully purchased skin ${skin.name}! This is your first skin, so skin has been equipped automatically!")
                                .setEphemeral(true)
                                .queue()
                        } else {
                            e.deferReply()
                                .setContent("Successfully purchased skin : ${skin.name}! You can equip skin in `cd.cards` command!")
                                .setEphemeral(true)
                                .queue()
                        }

                        goBack()
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    skin.purchase(purchaser, inventory, containers)

                    if (inventory.skins.filter { s -> s.card == skin.card }.size == 1) {
                        inventory.equippedSkins[skin.card] = skin

                        event.deferReply()
                            .setContent("Successfully purchased skin ${skin.name}! This is your first skin, so skin has been equipped automatically!")
                            .setEphemeral(true)
                            .queue()
                    } else {
                        event.deferReply()
                            .setContent("Successfully purchased skin : ${skin.name}! You can equip skin in `cd.cards` command!")
                            .setEphemeral(true)
                            .queue()
                    }

                    goBack()
                }
            }
            "cost" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values[0].toInt()
                val container = containers[index]

                connectTo(event, CardCostPayHolder(authorMessage, userID, channelID, message, container, containers))
            }
            "back" -> {
                if (containers.any { container -> container.pickedCards.isNotEmpty() }) {
                    StaticStore.removeHolder(authorMessage.author.id, this)

                    registerPopUp(event, "Are you sure you want to go back? All your selected cards will be cleared")

                    connectTo(ConfirmPopUpHolder(authorMessage, userID, channelID, message, { e ->
                        e.deferEdit().queue()

                        goBack()
                    }, CommonStatic.Lang.Locale.EN))
                } else {
                    event.deferEdit().queue()

                    goBack()
                }
            }
        }
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        var builder = event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        builder.queue()
    }

    private fun applyResult() {
        var builder = message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        builder.queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder()

        builder.append(skin.displayInfo(authorMessage.jda, false, true))

        if (!skin.cost.affordable(inventory)) {
            builder.append("\n\nYou can't afford this skin. Check reason below :\n\n").append(skin.cost.getReason(inventory))
        } else if (containers.any { container -> !container.paid() }) {
            builder.append("\n\nTo purchase this skin, you have to pay all card costs. You can check if you paid for each card cost or not by checking each list's descriptions\n" +
                    "\n" +
                    "Additionally, cat foods and platinum shards will be paid automatically")
        } else {
            builder.append("\n\nClick `Purchase` button to pay purchase the skin")
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        if (containers.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            containers.forEachIndexed { index, container ->
                options.add(
                    SelectOption.of(if (container.cost is SpecificCardCost) container.cost.simpleCostName() else container.cost.getCostName(), index.toString())
                        .withDescription(if (container.paid()) "Ready to pay" else "Need to be paid")
                )
            }

            result.add(ActionRow.of(StringSelectMenu.create("cost").addOptions(options).setPlaceholder("Select cost to pay cards").setDisabled(!skin.cost.affordable(
                inventory
            )).build()))
        }

        val buttons = ArrayList<Button>()

        buttons.add(
            Button.success("purchase", "Purchase")
                .withDisabled(!skin.cost.affordable(inventory) || containers.any { container -> !container.paid() })
                .withEmoji(EmojiStore.ABILITY["CF"])
        )

        buttons.add(Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK))

        result.add(ActionRow.of(buttons))

        return result
    }
}