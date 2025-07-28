package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.data.ConfigHolder
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SkinPurchaseSelectHolder(author: Message, userID: String, channelID: String, message: Message, private val card: Card) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private enum class FilterMode {
        NONE,
        PURCHASE_AMOUNT,
        SKIN_NAME,
        CAT_FOOD,
        PLATINUM_SHARDS
    }

    private val inventory = Inventory.getInventory(author.author.idLong)

    private val skins = CardData.skins.filter { skin -> skin.card == card && skin !in inventory.skins }.toMutableList()

    private var page = 0
    private var filterMode = FilterMode.NONE

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
            "skin" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values.first().toInt()

                val skin = skins[index]

                connectTo(event, SkinPurchasePayHolder(authorMessage, userID, channelID, message, skin))
            }
            "filter" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                filterMode = FilterMode.valueOf(event.values.first().toString())

                when(filterMode) {
                    FilterMode.NONE -> {
                        skins.clear()

                        skins.addAll(CardData.skins.filter { skin -> skin.card == card && skin !in inventory.skins })
                    }
                    FilterMode.PURCHASE_AMOUNT -> {
                        skins.sortBy { s -> return@sortBy CardData.inventories.values.count { i -> s in i.skins } }
                    }
                    FilterMode.SKIN_NAME -> {
                        skins.sortBy { s -> return@sortBy s.name }
                    }
                    FilterMode.CAT_FOOD -> {
                        skins.sortBy { s -> return@sortBy s.cost.catFoods }
                    }
                    FilterMode.PLATINUM_SHARDS -> {
                        skins.sortBy { s -> return@sortBy s.cost.platinumShards }
                    }
                }

                applyResult(event)
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

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
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

    private fun applyResult(event: IMessageEditCallback) {
        if (event !is GenericComponentInteractionCreateEvent)
            return

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
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(skins.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
                val authorName = if (skins[i].creator == -1L) {
                    "System"
                } else {
                    "<@" + skins[i].creator + ">"
                }

                builder.append(i + 1).append(". ").append(skins[i].name).append(" [By ").append(authorName).append("]").append("\n")
            }
        }

        if (!inventory.cards.containsKey(card) && !inventory.favorites.containsKey(card)) {
            val warning = Emoji.fromUnicode("⚠️").formatted

            builder.append("\n**").append(warning).append(" You don't own this card! It's recommended to purchase skin after owning the card ").append(warning).append(" **")
        }

        return builder.toString().trim()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        if (skins.isEmpty()) {
            options.add(SelectOption.of("A", "A"))
        } else {
            for (i in page * ConfigHolder.SearchLayout.COMPACTED.chunkSize until min(skins.size, (page + 1) * ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
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
                    "${skin.skinID} | $purchasable | $purchaseText"
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

        val filterModeOption = ArrayList< SelectOption>()

        for (filterMode in FilterMode.entries) {
            val labelName = when(filterMode) {
                FilterMode.NONE -> "None"
                FilterMode.PURCHASE_AMOUNT -> "By Purchased Amount"
                FilterMode.SKIN_NAME -> "By Skin Name"
                FilterMode.CAT_FOOD -> "By Cat Food Cost"
                FilterMode.PLATINUM_SHARDS -> "By Platinum Shard Cost"
            }

            filterModeOption.add(SelectOption.of(labelName, filterMode.name))
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("filter")
                .addOptions(filterModeOption)
                .setRequiredRange(1, 1)
                .setPlaceholder("Sort skin by")
                .setDisabled(skins.isEmpty())
                .setDefaultValues(filterMode.name)
                .build()
        ))

        if (skins.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
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