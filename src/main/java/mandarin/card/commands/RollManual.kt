package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.ManualRollSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.concurrent.CountDownLatch
import kotlin.math.ceil
import kotlin.math.min

class RollManual : Command(CommonStatic.Lang.Locale.EN, true) {
    @Throws(Exception::class)
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val g = loader.guild

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.hasAllPermission(m))
            return

        val contents = loader.content.split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(ch, "Not enough data! You have to pass members data to manually roll the pack!", loader.message
            ) { a -> a }

            return
        }

        val users = getUserID(loader.content, g)

        if (users.isEmpty()) {
            replyToMessageSafely(ch, "Bot failed to find user ID from command. User must be provided via either mention of user ID", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(ch, "Please select the pack that you want to roll", loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(m.id, ManualRollSelectHolder(loader.message, m.id, ch.id, msg, m, users))
        }
    }

    private fun getUserID(contents: String, g: Guild) : List<String> {
        val result = ArrayList<String>()

        val segments = contents.split(Regex(" "), 2)

        if (segments.size < 2)
            return result

        val filtered = segments[1].replace(Regex("-[slp]"), "").replace(" ", "").split(Regex(","))

        for(segment in filtered) {
            if (StaticStore.isNumeric(segment)) {
                result.add(segment)
            } else if (segment.startsWith("<@")) {
                result.add(segment.replace("<@", "").replace(">", ""))
            }
        }

        result.removeIf { id ->
            val waiter = CountDownLatch(1)
            var needRemoval = false

            g.retrieveMember(UserSnowflake.fromId(id)).queue({ _ ->
                waiter.countDown()
                needRemoval = false
            }, { _ ->
                waiter.countDown()
                needRemoval = true
            })

            waiter.await()

            needRemoval
        }

        return result
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val packOptions = ArrayList<SelectOption>()

        val size = min(CardData.cardPacks.size, SearchHolder.PAGE_CHUNK)

        for (i in 0 until size) {
            packOptions.add(SelectOption.of(CardData.cardPacks[i].packName, CardData.cardPacks[i].uuid))
        }

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

        if (CardData.cardPacks.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()
            val totalPage = ceil(CardData.cardPacks.size * 1.0 / SearchHolder.PAGE_CHUNK)

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

        result.add(ActionRow.of(Button.danger("close", "Close")))

        return result
    }
}
