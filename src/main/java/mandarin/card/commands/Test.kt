package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.UserSnowflake

class Test : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL)
            return

        val segments = loader.content.split(" ")

        if (segments.size < 2)
            return

        val id = segments[1].replace("<@", "").replace(">", "").toLong()

        val role = loader.guild.roles.find { r -> r.id == CardData.Role.LEGEND.id }

        if (role == null) {
            replyToMessageSafely(loader.channel, "Quack?", loader.message) { a -> a }

            return
        }

        loader.guild.removeRoleFromMember(UserSnowflake.fromId(id), role).queue()
    }
}