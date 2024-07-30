package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu

class TransferInventoryTargetHolder(
    author: Message,
    channelID: String,
    message: Message,
    private val sourceUser: Long,
    mode: CardData.TransferMode,
    r: Boolean
) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN) {
    var transferMode = mode
        private set
    var reset = r
        private set

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun onExpire() {
        message.editMessage("Inventory transfer expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "member" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val id = event.values[0].idLong

                if (id == sourceUser) {
                    event.deferReply().setContent("You can't transfer inventory to same user!").setEphemeral(true).queue()

                    return
                }

                val modeName = when (transferMode) {
                    CardData.TransferMode.INJECT -> "Inject"
                    CardData.TransferMode.OVERRIDE -> "Override"
                }

                val resetText = if (reset) {
                    "source user's inventory will get reset after transfer"
                } else {
                    "source user will keep their inventory after transfer"
                }

                registerPopUp(event, "Are you sure you want to transfer inventory from <@$sourceUser> to <@$id>? This cannot be undone.\n" +
                        "\n" +
                        "Mode is $modeName, and $resetText")

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message,{ e ->
                    val sourceInventory = Inventory.getInventory(sourceUser)
                    val targetInventory = Inventory.getInventory(id)

                    targetInventory.transferInventory(sourceInventory, transferMode)

                    if (reset) {
                        CardData.inventories.remove(sourceUser)
                    }

                    CardBot.saveCardData()

                    e.deferEdit()
                        .setContent("Successfully transferred the inventory!")
                        .setComponents()
                        .setAllowedMentions(ArrayList())
                        .mentionRepliedUser(false)
                        .queue()
                }, CommonStatic.Lang.Locale.EN))
            }
            "mode" -> {
                transferMode = CardData.TransferMode.entries[(transferMode.ordinal + 1) % CardData.TransferMode.entries.size]

                applyResult(event)
            }
            "reset" -> {
                reset = !reset

                applyResult(event)
            }
            "back" -> {
                goBack(event)
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Closed inventory transfer")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun clean() {

    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val cf = EmojiStore.ABILITY["CF"]?.formatted

        val modeExplanation = when(transferMode) {
            CardData.TransferMode.INJECT ->  "Inject mode will keep target user's contents, and add up contents to it.\n\n" +
                    " For example, if you want to transfer inventory of user A to user B, and if we assume that user A had $cf 5000 and user B had $cf 10000, after transfer is done, user B will have $cf 15000"
            CardData.TransferMode.OVERRIDE -> "Override mode will completely remove what target user had, and replace it to picked user's inventory.\n\n" +
                    " For example, if you want to transfer inventory of user A to user B, and if we assume that user A had $cf 5000 and user B had $cf 10000, after transfer is done, regardless of what user B had, user B will have $cf 5000"
        }

        val resetExplanation = if (reset) {
            "After transfer is done, picked user's inventory won't get reset, and keep the contents"
        } else {
            "After transfer is done, picked user's inventory will be wiped out"
        }

        return return "## Inventory Transfer\n" +
                "Now you have to pick target user who will get transferred to\n" +
                "### Source User : <@$sourceUser>\n" +
                "### Transfer Mode : Inject\n" +
                "$modeExplanation\n" +
                "### Reset User's Inventory? : No\n" +
                resetExplanation
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("member", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Pick user here")
                .setRequiredRange(1, 1)
                .build()
        ))

        val modeName = when(transferMode) {
            CardData.TransferMode.INJECT -> "Inject"
            CardData.TransferMode.OVERRIDE -> "Override"
        }

        result.add(ActionRow.of(
            Button.secondary("mode", "Mode : $modeName").withEmoji(Emoji.fromUnicode("🎛️")),
            Button.secondary("reset", "Reset User's Inventory").withEmoji(if (reset) EmojiStore.SWITCHON else EmojiStore.SWITCHOFF)
        ))

        result.add(ActionRow.of(
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK),
            Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)
        ))

        return result
    }
}