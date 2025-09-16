package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.holder.modal.CatFoodCostHolder
import mandarin.card.supporter.holder.modal.PlatinumShardCostHolder
import mandarin.card.supporter.pack.BannerCardCost
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
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.TimeUnit
import kotlin.math.min

class SkinCostManageHolder(author: Message, userID: String, channelID: String, message: Message, private val skin: Skin) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
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
            "cf" -> {
                val input = TextInput.create("amount", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Define amount of cat food cost")
                    .setValue(skin.cost.catFoods.toString())
                    .build()

                val modal = Modal.create("cf", "Cat Food Cost")
                    .addComponents(Label.of("Amount", input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(CatFoodCostHolder(authorMessage, userID, channelID, message, skin.cost))
            }
            "shard" -> {
                val input = TextInput.create("amount", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Define amount of platinum shard cost")
                    .setValue(skin.cost.platinumShards.toString())
                    .build()

                val modal = Modal.create("shard", "Platinum Shard Cost")
                    .addComponents(Label.of("Amount", input))
                    .build()

                event.replyModal(modal).queue()

                connectTo(PlatinumShardCostHolder(authorMessage, userID, channelID, message, skin.cost))
            }
            "add" -> {
                connectTo(event, SkinCostTypeHolder(authorMessage, userID, channelID, message, skin))
            }
            "cost" -> {
                if (event !is StringSelectInteractionEvent) {
                    return
                }

                val index = event.values[0].toInt()

                if (index < 0 || index >= skin.cost.cardsCosts.size)
                    return

                when(val cost = skin.cost.cardsCosts[index]) {
                    is BannerCardCost -> connectTo(event, SkinBannerCostHolder(authorMessage, userID, channelID, message, skin, cost, false))
                    is TierCardCost -> connectTo(event, SkinTierCostHolder(authorMessage, userID, channelID, message, skin, cost, false))
                    is SpecificCardCost -> connectTo(event, SkinSpecificCardCostHolder(authorMessage, userID, channelID, message, skin, cost, false))
                }
            }
            "role" -> {
                if (event !is EntitySelectInteractionEvent)
                    return

                val id = event.values[0].id

                if (id in skin.cost.roles) {
                    skin.cost.roles.remove(id)
                } else {
                    skin.cost.roles.add(id)
                }

                if (skin in CardData.skins) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "back" -> {
                if (skin in CardData.skins) {
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
        if (event !is GenericComponentInteractionCreateEvent)
            return

        var builder = event.deferEdit()
            .setContent(getCostManager())
            .setComponents(getCostManagerComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())

        if (event.message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun applyResult() {
        var builder = message.editMessage(getCostManager())
            .setComponents(getCostManagerComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)

        if (message.attachments.isNotEmpty()) {
            builder = builder.setFiles()
        }

        builder.queue()
    }

    private fun getCostManager() : String {
        val builder = StringBuilder("## Skin Cost Adjust Menu\nSkin name : ")
            .append(skin.skinID)
            .append("\n\n")

        if (skin.cost.cardsCosts.isEmpty() && skin.cost.catFoods == 0L && skin.cost.platinumShards == 0L && skin.cost.roles.isEmpty()) {
            builder.append("- This skin is free")
        } else {
            builder.append("Required Cat Food : ")
                .append(skin.cost.catFoods)
                .append(" ")
                .append(EmojiStore.ABILITY["CF"]?.formatted)
                .append("\nRequired Platinum Shard : ")
                .append(skin.cost.platinumShards)
                .append(" ")
                .append(EmojiStore.ABILITY["SHARD"]?.formatted)
                .append("\n\nRequired Cards\n\n")

            for (i in 0 until min(StringSelectMenu.OPTIONS_MAX_AMOUNT, skin.cost.cardsCosts.size)) {
                builder.append(i + 1).append(". ").append(skin.cost.cardsCosts[i].getCostName()).append("\n")
            }

            if (skin.cost.roles.isNotEmpty()) {
                builder.append("\nTo obtain this skin, users must have")

                if (skin.cost.roles.size > 1)
                    builder.append(" one of these roles below :\n\n")
                else
                    builder.append(" this role : ")

                for (i in skin.cost.roles.indices) {
                    builder.append("<@&")
                        .append(skin.cost.roles[i])
                        .append(">")

                    if (i < skin.cost.roles.size - 1)
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
            Button.secondary("add", "Add Card Cost").withEmoji(Emoji.fromUnicode("➕")).withDisabled(skin.cost.cardsCosts.size == StringSelectMenu.OPTIONS_MAX_AMOUNT && skin.cost.cardsCosts.any { cost -> cost is SpecificCardCost })
        ))

        if (skin.cost.cardsCosts.isNotEmpty()) {
            val options = ArrayList<SelectOption>()

            for (i in 0 until min(StringSelectMenu.OPTIONS_MAX_AMOUNT, skin.cost.cardsCosts.size)) {
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
            .setPlaceholder("Select role to require users to have to see the skin")
            .build()))

        result.add(ActionRow.of(Button.secondary("back", "Go Back")))

        return result
    }
}