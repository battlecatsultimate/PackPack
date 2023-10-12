package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.ModifyCategoryHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class ModifyInventory : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return
        val g = getGuild(event) ?: return

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val contents = getContent(event).split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "You have to provide member whose inventory will be managed!", getMessage(event)) { a -> a }

            return
        }

        val userID = getUserID(contents)

        if (userID.isBlank() || !StaticStore.isNumeric(userID)) {
            replyToMessageSafely(ch, "Bot failed to find user ID from command. User must be provided via either mention of user ID", getMessage(event)) { a -> a }

            return
        }

        try {
            val targetMember = g.retrieveMember(UserSnowflake.fromId(userID)).complete()

            if (targetMember.user.isBot) {
                replyToMessageSafely(ch, "You can't modify inventory of the bot!", getMessage(event)) { a -> a }

                return
            }

            val inventory = Inventory.getInventory(targetMember.id)

            val msg = getRepliedMessageSafely(ch, "Please select which thing you want to modify for inventory of ${targetMember.asMention}", getMessage(event)) { a -> a.setComponents(registerComponents()) }

            StaticStore.putHolder(m.id, ModifyCategoryHolder(getMessage(event), ch.id, msg, inventory, targetMember))
        } catch (_: Exception) {
            replyToMessageSafely(ch, "Bot failed to find provided user in this server", getMessage(event)) { a -> a }
        }
    }

    private fun getUserID(contents: List<String>) : String {
        for(segment in contents) {
            if (StaticStore.isNumeric(segment)) {
                return segment
            } else if (segment.startsWith("<@")) {
                return segment.replace("<@", "").replace(">", "")
            }
        }

        return ""
    }

    private fun registerComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val modeOptions = ArrayList<SelectOption>()

        modeOptions.add(SelectOption.of("Cards", "card"))
        modeOptions.add(SelectOption.of("Vanity Roles", "role"))

        val modes = StringSelectMenu.create("category")
            .addOptions(modeOptions)
            .setPlaceholder("Select category that you want to modify")
            .build()

        rows.add(ActionRow.of(modes))

        rows.add(ActionRow.of(Button.danger("close", "Close")))

        return rows
    }
}