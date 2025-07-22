package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

class CommandLockHolder(author: Message, userID: String, channelID: String, message: Message, private val classes: List<Class<*>>) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private var page = 0
        set(value) {
            field = value

            val totalPage = getTotalPage(classes.size)

            field = max(0, min(field, totalPage - 1))
        }

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Command locker expired")
            .setComponents()
            .setAllowedMentions(arrayListOf())
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
            "command" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val index = event.values.first().toInt()

                val command = classes[index]

                if (command in CardData.lockedCommands) {
                    CardData.lockedCommands.remove(command)
                } else {
                    CardData.lockedCommands.add(command)
                }

                applyResult(event)
            }
            "confirm" -> {
                event.deferEdit()
                    .setContent("Command locker closed")
                    .setComponents(ArrayList())
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        val builder = StringBuilder("Select command to lock/unlock\n\n")

        for (i in SearchHolder.PAGE_CHUNK * page until min(SearchHolder.PAGE_CHUNK * (page + 1), classes.size)) {
            val locked = if (classes[i] in CardData.lockedCommands) {
                EmojiStore.SWITCHOFF.formatted + " [Locked]"
            } else {
                EmojiStore.SWITCHON.formatted + " [Unlocked]"
            }

            builder.append(i + 1).append(". **cd.").append(classes[i].simpleName.lowercase()).append("** : ").append(locked).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val result = ArrayList<MessageTopLevelComponent>()

        val commandOptions = ArrayList<SelectOption>()

        for (i in page * SearchHolder.PAGE_CHUNK until min(SearchHolder.PAGE_CHUNK * (page + 1), classes.size)) {
            val locked = if (classes[i] in CardData.lockedCommands) {
                EmojiStore.SWITCHOFF
            } else {
                EmojiStore.SWITCHON
            }

            commandOptions.add(SelectOption.of("cd.${classes[i].simpleName.lowercase()}", i.toString()).withEmoji(locked))
        }

        result.add(
            ActionRow.of(
            StringSelectMenu.create("command").addOptions(commandOptions).setPlaceholder("Select command to lock/unlock").build()
        ))

        val totalPage = SearchHolder.getTotalPage(classes.size)

        if (classes.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(
            ActionRow.of(
            Button.primary("confirm", "Confirm")
        ))

        return result
    }
}