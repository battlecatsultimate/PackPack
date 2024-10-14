package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.math.min

class EquipHolder(author: Message, userID: String, channelID: String, message: Message, private val inventory: Inventory) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Equipment expired")
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
            "close" -> {
                event.deferEdit()
                    .setContent("Closed equipping page")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
            else -> {
                if (event !is ButtonInteractionEvent)
                    return

                if (!event.componentId.startsWith("role/"))
                    return

                val selectedRole = CardData.Role.valueOf(event.componentId.replace("role/", ""))

                val g = event.guild ?: return

                val role = g.roles.find { r -> r.id == selectedRole.id } ?: return

                val m = event.member ?: return

                if (m.roles.contains(role)) {
                    g.removeRoleFromMember(UserSnowflake.fromId(m.id), role).queue {
                        event.deferReply()
                            .setContent("Successfully unequipped ${role.asMention}")
                            .setAllowedMentions(ArrayList())
                            .setEphemeral(true)
                            .queue()
                    }
                } else {
                    g.addRoleToMember(UserSnowflake.fromId(m.id), role).queue {
                        event.deferReply()
                            .setContent("Successfully equipped ${role.asMention}")
                            .setAllowedMentions(ArrayList())
                            .setEphemeral(true)
                            .queue()
                    }
                }

                g.retrieveMember(UserSnowflake.fromId(m.id)).queue { member ->
                    applyResult(g, member)
                }
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        val g = event.guild ?: return
        val m = event.member ?: return

        event.deferEdit()
            .setContent(getText(g, m))
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(getComponents(g, m))
            .queue()
    }

    private fun applyResult(g: Guild, m: Member) {
        message.editMessage(getText(g, m))
            .mentionRepliedUser(false)
            .setAllowedMentions(ArrayList())
            .setComponents(getComponents(g, m))
            .queue()
    }

    private fun getText(g: Guild, m: Member) : String {
        val builder = StringBuilder("Purchased vanity roles of ${m.asMention}\n\n")

        if (inventory.vanityRoles.isEmpty()) {
            builder.append("- No Roles")
        } else {
            for (i in page * 3 until min(inventory.vanityRoles.size, (page + 1) * 3)) {
                val r = g.roles.find { r -> r.id == inventory.vanityRoles[i].id }

                if (r == null) {
                    builder.append("- ").append(inventory.vanityRoles[i].title)
                } else {
                    val equipped = if (m.roles.contains(r)) {
                        "Equipped"
                    } else {
                        "Unequipped"
                    }

                    builder.append("- ")
                        .append(inventory.vanityRoles[i].title)
                        .append(" : ")
                        .append(equipped)
                }

                builder.append("\n")
            }
        }

        return builder.toString()
    }

    private fun getComponents(g: Guild, m: Member) : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val roles = inventory.vanityRoles

        for (i in page * 3 until min(roles.size, (page + 1) * 3)) {
            val r = g.roles.find { r -> r.id == roles[i].id }

            if (r == null) {
                rows.add(ActionRow.of(Button.secondary(roles[i].title, "role/${roles[i].name}").withEmoji(EmojiStore.ABILITY[roles[i].id])))
            } else {
                val equipped = if (m.roles.contains(r)) {
                    "Equipped"
                } else {
                    "Unequipped"
                }

                rows.add(
                    ActionRow.of(
                        Button.secondary("role/${roles[i].name}", "${roles[i].title} : $equipped").withEmoji(
                            EmojiStore.ABILITY[roles[i].key])))
            }
        }
        val dataSize = roles.size

        var totPage = dataSize / 3

        if (dataSize % 3 != 0)
            totPage++

        if (dataSize > 3) {
            val buttons = ArrayList<Button>()

            if(totPage > 10) {
                if(page - 10 < 0) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS))
                }
            }

            if(page - 1 < 0) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS))
            }

            if(page + 1 >= totPage) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).asDisabled())
            } else {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))
            }

            if(totPage > 10) {
                if(page + 10 >= totPage) {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).asDisabled())
                } else {
                    buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
                }
            }

            rows.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.primary("close", "Close"))

        rows.add(ActionRow.of(confirmButtons))

        return rows
    }
}