package mandarin.card.supporter.holder.pack

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.CatFoodCostHolder
import mandarin.card.supporter.holder.modal.PlatinumShardCostHolder
import mandarin.card.supporter.pack.BannerCardCost
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.card.supporter.pack.TierCardCost
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.min

class CardPackCostHolder(author: Message, userID: String, channelID: String, message: Message, private val pack: CardPack) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Card pack manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "cf" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Define amount of cat food cost")
                    .setValue(pack.cost.catFoods.toString())
                    .build()

                val modal = Modal.create("cf", "Cat Food Cost")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(CatFoodCostHolder(authorMessage, userID, channelID, message, pack.cost))
            }
            "shard" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Define amount of platinum shard cost")
                    .setValue(pack.cost.platinumShards.toString())
                    .build()

                val modal = Modal.create("shard", "Platinum Shard Cost")
                    .addComponents(ActionRow.of(input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(PlatinumShardCostHolder(authorMessage, userID, channelID, message, pack.cost))
            }
            "add" -> {
                connectTo(event, CardCostTypeHolder(authorMessage, userID, channelID, message, pack))
            }
            "cost" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                val index = event.values[0].toInt()

                if (index < 0 || index >= pack.cost.cardsCosts.size)
                    return

                when(val cost = pack.cost.cardsCosts[index]) {
                    is BannerCardCost -> connectTo(event, BannerCostHolder(authorMessage, userID, channelID, message, pack, cost, false))
                    is TierCardCost -> connectTo(event, TierCostHolder(authorMessage, userID, channelID, message, pack, cost, false))
                    is SpecificCardCost -> connectTo(event, SpecificCardCostHolder(authorMessage, userID, channelID, message, pack, cost, false))
                }
            }
            "role" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val id = event.values[0].id

                if (id in pack.cost.roles) {
                    pack.cost.roles.remove(id)
                } else {
                    pack.cost.roles.add(id)
                }

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "back" -> {
                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferEdit().queue()

                goBack()
            }
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        applyResult(event)
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getCostManager())
            .setComponents(getCostManagerComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getCostManager())
            .setComponents(getCostManagerComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getCostManager() : String {
        val builder = StringBuilder("## Pack Cost Adjust Menu\nPack name : ")
            .append(pack.packName)
            .append("\n\n")

        if (pack.cost.cardsCosts.isEmpty() && pack.cost.catFoods == 0L && pack.cost.platinumShards == 0L && pack.cost.roles.isEmpty()) {
            builder.append("- This pack is free")
        } else {
            builder.append("Required Cat Food : ")
                .append(pack.cost.catFoods)
                .append(" ")
                .append(EmojiStore.ABILITY["CF"]?.formatted)
                .append("\nRequired Platinum Shard : ")
                .append(pack.cost.platinumShards)
                .append(" ")
                .append(EmojiStore.ABILITY["SHARD"]?.formatted)
                .append("\n\nRequired Cards\n\n")

            for (i in 0 until min(StringSelectMenu.OPTIONS_MAX_AMOUNT, pack.cost.cardsCosts.size)) {
                builder.append(i + 1).append(". ").append(pack.cost.cardsCosts[i].getCostName()).append("\n")
            }

            if (pack.cost.roles.isNotEmpty()) {
                builder.append("\nTo roll this pack, users must have")

                if (pack.cost.roles.size > 1)
                    builder.append(" one of these roles below :\n\n")
                else
                    builder.append(" this role : ")

                for (i in pack.cost.roles.indices) {
                    builder.append("<@&")
                        .append(pack.cost.roles[i])
                        .append(">")

                    if (i < pack.cost.roles.size - 1)
                        builder.append(", ")
                }
            }
        }

        return builder.toString()
    }

    private fun getCostManagerComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        result.add(ActionRow.of(
            Button.secondary("cf", "Adjust Cat Food").withEmoji(EmojiStore.ABILITY["CF"]),
            Button.secondary("shard", "Adjust Platinum Shard").withEmoji(EmojiStore.ABILITY["SHARD"]),
            Button.secondary("add", "Add Card Cost").withEmoji(Emoji.fromUnicode("➕")).withDisabled(pack.cost.cardsCosts.size == StringSelectMenu.OPTIONS_MAX_AMOUNT && pack.cost.cardsCosts.any { cost -> cost is SpecificCardCost })
        ))

        if (pack.cost.cardsCosts.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            for (i in 0 until min(StringSelectMenu.OPTIONS_MAX_AMOUNT, pack.cost.cardsCosts.size)) {
                options.add(SelectOption.of((i + 1).toString(), i.toString()))
            }

            result.add(ActionRow.of(
                StringSelectMenu.create("cost")
                    .addOptions(options)
                    .setPlaceholder("Select cost to adjust")
                    .build()
            ))
        }

        result.add(ActionRow.of(EntitySelectMenu.create("role", EntitySelectMenu.SelectTarget.ROLE)
            .setPlaceholder("Select role to require users to have to see the pack")
            .build()))

        result.add(ActionRow.of(Button.secondary("back", "Go Back")))

        return result
    }
}