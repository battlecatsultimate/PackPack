package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.holder.moderation.modify.ModifyCategoryHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu

class ModifyInventory : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val g = loader.guild

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "You have to provide member whose inventory will be managed!", loader.message) { a -> a }

            return
        }

        val userID = getUserID(contents)

        if (userID.isBlank() || !StaticStore.isNumeric(userID)) {
            replyToMessageSafely(ch, "Bot failed to find user ID from command. User must be provided via either mention of user ID", loader.message) { a -> a }

            return
        }

        try {
            g.retrieveMember(UserSnowflake.fromId(userID)).queue { targetMember ->
                val inventory = Inventory.getInventory(targetMember.idLong)

                replyToMessageSafely(ch, "Please select which thing you want to modify for inventory of ${targetMember.asMention}", loader.message, { a ->
                    a.setComponents(registerComponents())
                }, { msg ->
                    StaticStore.putHolder(m.id, ModifyCategoryHolder(loader.message, m.id, ch.id, msg, inventory, targetMember))
                })
            }
        } catch (_: Exception) {
            replyToMessageSafely(ch, "Bot failed to find provided user in this server", loader.message) { a -> a }
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

    private fun registerComponents() : List<MessageTopLevelComponent> {
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