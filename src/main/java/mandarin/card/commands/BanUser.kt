package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.UserBanHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu
import kotlin.math.ceil
import kotlin.math.min

class BanUser : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }, { msg ->
            StaticStore.putHolder(m.id, UserBanHolder(loader.message, loader.channel.id, msg))
        })
    }

    private fun getContents() : String {
        val builder = StringBuilder()

        builder.append("Select user to ban from using any commands of this bot. Select banned user again to unban them\n")
            .append("### List of Banned Users\n")

        if (CardData.bannedUser.isEmpty()) {
            builder.append("- No Banned Users")
        } else {
            for (i in 0 until min(CardData.bannedUser.size, SearchHolder.PAGE_CHUNK)) {
                builder.append(i + 1).append(". <@").append(CardData.bannedUser[i]).append("> [").append(CardData.bannedUser[i]).append("]\n")
            }
        }

        return builder.toString().trim()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(
            EntitySelectMenu.create("user", EntitySelectMenu.SelectTarget.USER)
                .setPlaceholder("Select User To Ban/Unban")
                .setRequiredRange(1, EntitySelectMenu.OPTIONS_MAX_AMOUNT)
                .build()
        ))

        if (CardData.bannedUser.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(CardData.bannedUser.size * 1.0 / SearchHolder.PAGE_CHUNK).toInt()

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

        result.add(ActionRow.of(
            Button.primary("confirm", "Confirm").withEmoji(EmojiStore.CHECK)
        ))

        return result
    }
}