package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.CommandLockHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.min

class LockCommand : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val pkg = this.javaClass.`package`

        val classLoader = Thread.currentThread().contextClassLoader

        val stream = classLoader.getResourceAsStream(pkg.name.replace(".", "/")) ?: return

        val classList: List<Class<*>>

        BufferedReader(InputStreamReader(stream)).use { r ->
            classList = r.lines()
                .filter { l -> !l.contains("$") }
                .map { l -> pkg.name + "." + l.replace(Regex("\\.class$"), "") }
                .map { l -> Class.forName(l) }
                .filter { c -> c != this.javaClass }
                .toList().filterNotNull()
        }

        replyToMessageSafely(loader.channel, getContents(classList), loader.message, { a -> a.setComponents(getComponents(classList)) }) { msg ->
            StaticStore.putHolder(m.id, CommandLockHolder(loader.message, m.id, loader.channel.id, msg, classList))
        }
    }

    private fun getContents(classes: List<Class<*>>) : String {
        val builder = StringBuilder("Select command to lock/unlock\n\n")

        for (i in 0 until min(SearchHolder.PAGE_CHUNK, classes.size)) {
            val locked = if (classes[i] in CardData.lockedCommands) {
                EmojiStore.SWITCHOFF.formatted + " [Locked]"
            } else {
                EmojiStore.SWITCHON.formatted + " [Unlocked]"
            }

            builder.append(i + 1).append(". **cd.").append(classes[i].simpleName.lowercase()).append("** : ").append(locked).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents(classes: List<Class<*>>) : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val commandOptions = ArrayList<SelectOption>()

        for (i in 0 until min(SearchHolder.PAGE_CHUNK, classes.size)) {
            val locked = if (classes[i] in CardData.lockedCommands) {
                EmojiStore.SWITCHOFF
            } else {
                EmojiStore.SWITCHON
            }

            commandOptions.add(SelectOption.of("cd.${classes[i].simpleName.lowercase()}", i.toString()).withEmoji(locked))
        }

        result.add(ActionRow.of(
            StringSelectMenu.create("command").addOptions(commandOptions).setPlaceholder("Select command to lock/unlock").build()
        ))

        val totalPage = SearchHolder.getTotalPage(classes.size)

        if (classes.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

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
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}