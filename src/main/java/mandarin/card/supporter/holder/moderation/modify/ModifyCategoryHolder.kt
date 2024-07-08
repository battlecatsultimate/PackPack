package mandarin.card.supporter.holder.moderation.modify

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class ModifyCategoryHolder(author: Message, channelID: String, private val message: Message, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message) {
    override fun clean() {

    }

    override fun onExpire(id: String?) {

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

                        connectTo(event, ModifyModeSelectHolder(authorMessage, channelID, message, category, inventory, targetMember))
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

                expired = true

                expire(authorMessage.author.id)
            }
        }
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    private fun applyResult() {
        message.editMessage("Select category that you want to modify")
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
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