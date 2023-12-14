package mandarin.card.supporter.holder

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class ModifyCategoryHolder(author: Message, channelID: String, private val message: Message, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message.id) {
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
                    "cf" -> {
                        val target = when (event.values[0]) {
                            "card" -> "cards"
                            "role" -> "roles"
                            "cf" -> "cat foods"
                            else -> "platinum shards"
                        }

                        val content = if (event.values[0] == "cf") {
                            "Do you want to add or remove $target?\n\nCurrently this user has ${EmojiStore.ABILITY["CF"]?.formatted} ${inventory.catFoods}"
                        } else {
                            "Do you want to add or remove $target?"
                        }

                        event.deferEdit()
                            .setContent(content)
                            .setComponents(getComponents())
                            .mentionRepliedUser(false)
                            .queue()

                        expired = true

                        expire(authorMessage.author.id)

                        val category = when(event.values[0]) {
                            "card" -> CardData.ModifyCategory.CARD
                            "role" -> CardData.ModifyCategory.ROLE
                            "cf" -> CardData.ModifyCategory.CF
                            else -> CardData.ModifyCategory.SHARD
                        }

                        StaticStore.putHolder(authorMessage.author.id, ModifyModeSelectHolder(authorMessage, channelID, message, category, inventory, targetMember))
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

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Add", "add").withEmoji(Emoji.fromUnicode("➕")))
        modeOptions.add(SelectOption.of("Remove", "remove").withEmoji(Emoji.fromUnicode("➖")))

        val modes = StringSelectMenu.create("mode")
            .addOptions(modeOptions)
            .setPlaceholder("Please select mode")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(
            Button.secondary("back", "Back"),
            Button.danger("close", "Close")
        ))

        return rows
    }
}