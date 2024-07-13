package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.MessageUpdater
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class SkinPurchaseSelectHolder(author: Message, channelID: String, message: Message, private val card: Card) : ComponentHolder(author, channelID, message, CommonStatic.Lang.Locale.EN), MessageUpdater {
    private val inventory = Inventory.getInventory(author.author.idLong)

    private val skins = CardData.skins.filter { skin -> skin.card == card && skin !in inventory.skins }.toMutableList()

    private var page = 0

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "skin" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values.first().toInt()

                val skin = skins[index]

                connectTo(event, SkinPurchasePayHolder(authorMessage, channelID, message, skin))
            }
            "back" -> {
                goBack(event)
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
        }
    }

    override fun onMessageUpdated(message: Message) {
        this.message = message
    }

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(event: GenericComponentInteractionCreateEvent, child: Holder) {
        skins.clear()

        skins.addAll(CardData.skins.filter { skin -> skin.card == card && skin !in inventory.skins })

        applyResult(event)
    }

    override fun onBack(child: Holder) {
        skins.clear()

        skins.addAll(CardData.skins.filter { skin -> skin.card == card && skin !in inventory.skins })

        applyResult()
    }

    private fun applyResult() {
        var builder = message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        var builder = event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (event.message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Please select skin that you what to purchase\n### Selected card : ")
            .append(card.simpleCardInfo()).append("\n### List of Skin\n")

        if (skins.isEmpty()) {
            builder.append("- You owned all skins of this card!\n")
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". ").append(skins[i].name).append("\n")
            }
        }

        if (!inventory.cards.containsKey(card) && !inventory.favorites.containsKey(card)) {
            val warning = Emoji.fromUnicode("⚠️").formatted

            builder.append("\n**").append(warning).append(" You don't own this card! It's recommended to purchase skin after owning the card ").append(warning).append(" **")
        }

        return builder.toString().trim()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        if (skins.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * SearchHolder.PAGE_CHUNK until min(skins.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
                val skin = skins[i]

                val purchasedSize = CardData.inventories.values.count { i -> skin in i.skins }

                val purchaseText = if (purchasedSize > 2) {
                    "$purchasedSize Users Purchased"
                } else if (purchasedSize == 1) {
                    "1 User Purchased"
                } else {
                    ""
                }

                val purchasable = if (skins[i].cost.affordable(inventory)) {
                    "Purchasable"
                } else {
                    "Can't Be Purchased"
                }

                val description = if (purchaseText.isBlank()) {
                    "${skin.skinID} | $purchasable"
                } else {
                    "${skin.skinID} | $purchasable | $purchasable"
                }

                options.add(SelectOption.of(skins[i].name, i.toString()).withDescription(description))
            }
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("skin")
                .addOptions(options)
                .setRequiredRange(1, 1)
                .setPlaceholder("Select skin to purchase")
                .setDisabled(skins.isEmpty())
                .build()
        ))

        if (skins.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = getTotalPage(skins.size)

            if (totalPage > 10) {
                buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.secondary("prev", "Previous Pages").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(
            Button.secondary("back", "Go Back").withEmoji(EmojiStore.BACK)
        ))

        return result
    }
}