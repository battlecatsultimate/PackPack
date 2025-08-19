package mandarin.card.supporter.holder.slot

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Member
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
import kotlin.math.ceil
import kotlin.math.min

class SlotMachineSelectHolder(author: Message, userID: String, channelID: String, message: Message, member: Member, private val skip: Boolean, private val yes: Boolean) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    private val roles = member.roles.map { r -> r.idLong }
    private val possibleSlotMachines = CardData.slotMachines.filter { s -> s.valid && s.activate && (s.roles.isEmpty() || s.roles.any { r -> r in roles }) }

    private var page = 0

    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Slot machine select expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "slot" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val slotMachine = possibleSlotMachines[event.values[0].toInt()]

                connectTo(event, SlotMachineConfirmHolder(authorMessage, userID, channelID, message, slotMachine, skip, yes))
            }
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
                    .setContent("Closed slot machine selection!")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()

                end(true)
            }
        }
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent("Select slot machine to roll")
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<MessageTopLevelComponent> {
        val cooldownMap = CardData.slotCooldown.computeIfAbsent(authorMessage.author.idLong) { HashMap() }

        val currentTime = CardData.getUnixEpochTime()

        val result = ArrayList<MessageTopLevelComponent>()

        val options = ArrayList<SelectOption>()

        for (i in page * SearchHolder.PAGE_CHUNK until min(possibleSlotMachines.size, (page + 1) * SearchHolder.PAGE_CHUNK)) {
            val cooldown = cooldownMap[possibleSlotMachines[i].uuid]

            var option = SelectOption.of(possibleSlotMachines[i].name, i.toString())

            if (cooldown != null && currentTime - cooldown < 0) {
                option = option.withDescription("Cooldown Left : ${CardData.convertMillisecondsToText(cooldown - currentTime)}")
            }

            options.add(option)
        }

        result.add(ActionRow.of(StringSelectMenu.create("slot").addOptions(options).setPlaceholder("Select Slot Machine To Roll").build()))

        if (possibleSlotMachines.size > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            val totalPage = ceil(possibleSlotMachines.size * 1.0 / SearchHolder.PAGE_CHUNK)

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

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}