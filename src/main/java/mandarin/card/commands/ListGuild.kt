package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.holder.moderation.GuildListHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.data.ConfigHolder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import kotlin.math.ceil
import kotlin.math.min

class ListGuild : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL)
            return

        val jda = loader.client

        replyToMessageSafely(loader.channel, getContents(jda), loader.message, { a -> a.setComponents(getComponents(jda)) }) { msg ->
            StaticStore.putHolder(m.id, GuildListHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    private fun getContents(jda: JDA) : String {
        val builder = StringBuilder("### List of Guilds\n\n")

        val guilds = jda.guilds

        for (i in 0 until min(guilds.size, ConfigHolder.SearchLayout.COMPACTED.chunkSize)) {
            val g = guilds[i]

            builder.append(i + 1).append(". ").append(g.name).append(" - ").append(g.id).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents(jda: JDA) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val guilds = jda.guilds

        if (guilds.size > ConfigHolder.SearchLayout.COMPACTED.chunkSize) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(guilds.size * 1.0 / ConfigHolder.SearchLayout.COMPACTED.chunkSize).toInt()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        return result
    }
}