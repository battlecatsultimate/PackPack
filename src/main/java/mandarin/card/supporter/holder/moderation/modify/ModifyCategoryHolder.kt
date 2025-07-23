package mandarin.card.supporter.holder.moderation.modify

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu

class ModifyCategoryHolder(author: Message, userID: String, channelID: String, message: Message, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Inventory modification expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "category" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                when(event.values[0]) {
                    "card",
                    "role",
                    "skin",
                    "cf",
                    "shard" -> {
                        val category = when(event.values[0]) {
                            "card" -> CardData.ModifyCategory.CARD
                            "role" -> CardData.ModifyCategory.ROLE
                            "skin" -> CardData.ModifyCategory.SKIN
                            "cf" -> CardData.ModifyCategory.CF
                            else -> CardData.ModifyCategory.SHARD
                        }

                        connectTo(event, ModifyModeSelectHolder(authorMessage, userID, channelID, message, category, inventory, targetMember))
                    }
                }
            }
            "close" -> {
                event.deferEdit()
                    .setContent("Modify closed")
                    .setComponents()
                    .mentionRepliedUser(false)
                    .setAllowedMentions(ArrayList())
                    .queue()

                end(true)
            }
        }
    }

    override fun onBack(child: Holder) {
        applyResult()
    }

    private fun applyResult() {
        message.editMessage("Select category that you want to modify")
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Cards", "card").withEmoji(EmojiStore.ABILITY["CARD"]))
        modeOptions.add(SelectOption.of("Vanity Roles", "role").withEmoji(EmojiStore.DOGE))
        modeOptions.add(SelectOption.of("Skin", "skin").withEmoji(EmojiStore.ABILITY["SKIN"]))
        modeOptions.add(SelectOption.of("Cat Foods", "cf").withEmoji(EmojiStore.ABILITY["CF"]))
        modeOptions.add(SelectOption.of("Platinum Shards", "shard").withEmoji(EmojiStore.ABILITY["SHARD"]))

        val modes = StringSelectMenu.create("category")
            .addOptions(modeOptions)
            .setPlaceholder("Select category that you want to modify")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(Button.danger("close", "Close")))

        return rows
    }
}