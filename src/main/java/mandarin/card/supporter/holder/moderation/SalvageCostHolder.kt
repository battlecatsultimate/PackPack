package mandarin.card.supporter.holder.moderation

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.modal.SalvageCostModifyHolder
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal

class SalvageCostHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val size = 2

    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Config expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "prev" -> {
                page--

                applyResult(event)
            }
            "next" -> {
                page++

                applyResult(event)
            }
            "t1",
            "t2",
            "seasonal",
            "collab",
            "t3",
            "t4" -> {
                val salvageMode = when(event.componentId) {
                    "t1" -> CardData.SalvageMode.T1
                    "t2" -> CardData.SalvageMode.T2
                    "seasonal" -> CardData.SalvageMode.SEASONAL
                    "collab" -> CardData.SalvageMode.COLLAB
                    "t3" -> CardData.SalvageMode.T3
                    else -> CardData.SalvageMode.T4
                }

                val input = TextInput.create("cost", TextInputStyle.SHORT)
                    .setPlaceholder("Define amount of platinum shards that will be given upon salvage")
                    .setRequired(true)
                    .setValue(salvageMode.cost.toString())
                    .build()

                val modal = Modal.create("salvageCost", "Cost of Salvaging")
                    .addComponents(Label.of("Cost", input))
                    .build()

                event.replyModal(modal).queue()

                StaticStore.putHolder(authorMessage.author.id, SalvageCostModifyHolder(authorMessage, userID, channelID, message, salvageMode) {
                    applyResult()
                })
            }
            "confirm" -> {
                event.deferEdit()
                    .setContent("Confirmed modification!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        return "Select button to adjust the amount of ${EmojiStore.ABILITY["SHARD"]?.formatted} platinum shards that will be given after salvaging\n" +
                "\n" +
                "### Salvage cost\n" +
                "\n" +
                "- Tier 1 [Common] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T1.cost}\n" +
                "- Regular Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T2.cost}\n" +
                "- Seasonal Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.SEASONAL.cost}\n" +
                "- Collaboration Tier 2 [Uncommon] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.COLLAB.cost}\n" +
                "- Tier 3 [Ultra Rare (Exclusives)] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T3.cost}\n" +
                "- Tier 4 [Legend Rare] : ${EmojiStore.ABILITY["SHARD"]?.formatted} ${CardData.SalvageMode.T4.cost}\n" +
                "\n" +
                "Page : ${page + 1} / $size"
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        when (page) {
            0 -> {
                result.add(ActionRow.of(Button.secondary("t1", "Tier 1 [Common]")))
                result.add(ActionRow.of(Button.secondary("t2", "Regular Tier 2 [Uncommon]")))
                result.add(ActionRow.of(Button.secondary("seasonal", "Seasonal Tier 2 [Uncommon]")))
            }
            1 -> {
                result.add(ActionRow.of(Button.secondary("collab", "Collaboration Tier 2 [Uncommon]")))
                result.add(ActionRow.of(Button.secondary("t3", "Tier 3 [Ultra Rare (Exclusives)]")))
                result.add(ActionRow.of(Button.secondary("t4", "Tier 4 [Legend Rare]")))
            }
        }

        result.add(
            ActionRow.of(
                Button.secondary("prev", "Previous").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0),
                Button.secondary("next", "Next").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= size)
            )
        )

        result.add(ActionRow.of(Button.primary("confirm", "Confirm")))

        return result
    }
}