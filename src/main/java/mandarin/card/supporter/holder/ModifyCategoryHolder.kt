package mandarin.card.supporter.holder

import mandarin.card.supporter.Inventory
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
                    "role" -> {
                        event.deferEdit()
                            .setContent("Do you want to add cards or remove ${if (event.values[0] == "card") "cards" else "roles"}?")
                            .setComponents(getComponents())
                            .mentionRepliedUser(false)
                            .queue()

                        expired = true

                        expire(authorMessage.author.id)

                        StaticStore.putHolder(authorMessage.author.id, ModifyModeSelectHolder(authorMessage, channelID, message, event.values[0] == "card", inventory, targetMember))
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