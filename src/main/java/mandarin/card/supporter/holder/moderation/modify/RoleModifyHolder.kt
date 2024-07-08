package mandarin.card.supporter.holder.moderation.modify

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.log.TransactionLogger
import mandarin.packpack.supporter.EmojiStore
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

class RoleModifyHolder(author: Message, channelID: String, private val message: Message, private val isAdd: Boolean, private val inventory: Inventory, private val targetMember: Member) : ComponentHolder(author, channelID, message.id) {
    private val roles = ArrayList<CardData.Role>(
        if (isAdd) {
            CardData.Role.entries.filter { r -> r !in inventory.vanityRoles }
        } else {
            inventory.vanityRoles
        }
    )

    private val selectedRoles = ArrayList<CardData.Role>()

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "role" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                if (event.values.isEmpty())
                    return

                val name = event.values[0]

                selectedRoles.add(CardData.Role.valueOf(name))

                filterRoles()

                applyResult(event)
            }
            "confirm" -> {
                if (isAdd) {
                    inventory.vanityRoles.addAll(selectedRoles)
                } else {
                    inventory.vanityRoles.removeAll(selectedRoles.toSet())
                }

                val m = event.member ?: return

                TransactionLogger.logRolesModify(selectedRoles, m, targetMember, isAdd, !isAdd && inventory.vanityRoles.isNotEmpty() && selectedRoles.size == inventory.vanityRoles.size)

                selectedRoles.clear()

                event.deferReply()
                    .setContent("Successfully ${if (isAdd) "added" else "removed"} selected roles!")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "remove" -> {
                selectedRoles.clear()

                selectedRoles.addAll(inventory.vanityRoles)

                event.deferReply()
                    .setContent("Selected all roles this user has. This will remove all roles from user. Please confirm this action")
                    .setEphemeral(true)
                    .queue()

                applyResult()
            }
            "clear" -> {
                selectedRoles.clear()

                event.deferReply()
                    .setContent("Successfully cleared selected roles!")
                    .setEphemeral(true)
                    .queue()

                filterRoles()

                applyResult()
            }
            "back" -> {
                event.deferEdit().queue()

                goBack()
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

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        filterRoles()

        applyResult(event)
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun applyResult() {
        message.editMessage(getText())
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(assignComponents())
            .queue()
    }

    private fun filterRoles() {
        roles.clear()

        roles.addAll(
            if (isAdd) {
                CardData.Role.entries.filter { r -> r !in inventory.vanityRoles && r != CardData.Role.NONE }.filter { r -> r !in selectedRoles }
            } else {
                inventory.vanityRoles.filter { r -> r !in selectedRoles }
            }
        )
    }

    private fun assignComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val roleOptions = ArrayList<SelectOption>()

        if (roles.isNotEmpty()) {
            for (role in roles) {
                roleOptions.add(SelectOption.of(role.title, role.name).withEmoji(EmojiStore.ABILITY[role.key]))
            }
        } else {
            roleOptions.add(SelectOption.of("a", "a"))
        }

        val roleMenu = StringSelectMenu.create("role")
            .addOptions(roleOptions)
            .setPlaceholder(
                if (roles.isEmpty()) {
                    "No roles to select"
                } else {
                    "Select role to be ${if (isAdd) "added" else "removed"}"
                }
            )
            .setDisabled(roles.isEmpty())
            .build()

        rows.add(ActionRow.of(roleMenu))

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.success("confirm", "Confirm").withDisabled(selectedRoles.isEmpty()))
        confirmButtons.add(Button.danger("clear", "Clear").withDisabled(selectedRoles.isEmpty()))

        if (!isAdd) {
            confirmButtons.add(Button.danger("remove", "Mass Remove").withDisabled(inventory.vanityRoles.isEmpty() || inventory.vanityRoles.size == selectedRoles.size))
        }

        confirmButtons.add(Button.secondary("back", "Back"))
        confirmButtons.add(Button.danger("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }

    private fun getText() : String {
        val builder = StringBuilder(
            if (isAdd)
                "Please select roles that will be added"
            else
                "Please select roles that will be removed"
        )

        builder.append("\n\n### Selected Cards\n\n")

        if (selectedRoles.isEmpty()) {
            builder.append("- None\n")
        } else {
            for (role in selectedRoles.toSet()) {
                builder.append("- ")
                    .append(role.title)
                    .append("\n")
            }
        }

        builder.append("\n```md\n")

        if (roles.isNotEmpty()) {
            roles.forEachIndexed { i, role ->
                builder.append(i + 1)
                    .append(". ")
                    .append(role.title)
                    .append("\n")
            }
        } else {
            builder.append("No Roles To Select")
        }

        builder.append("```")

        return builder.toString()
    }
}