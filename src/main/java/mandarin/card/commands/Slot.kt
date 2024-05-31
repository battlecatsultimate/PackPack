package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SlotMachineSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.ceil
import kotlin.math.min

class Slot : Command(LangID.EN, true){
    private val possibleSlotMachines = CardData.slotMachines.filter { s -> s.valid && s.activate }

    override fun doSomething(loader: CommandLoader) {
        replyToMessageSafely(loader.channel, "Select slot machine to roll", loader.message, { a -> a.setComponents(getComponents(loader.user)) }) { msg ->
            StaticStore.putHolder(loader.user.id, SlotMachineSelectHolder(loader.message, loader.channel.id, msg))
        }
    }

    private fun getComponents(user: User) : List<LayoutComponent> {
        val cooldownMap = CardData.slotCooldown.computeIfAbsent(user.id) { HashMap() }

        val currentTime = CardData.getUnixEpochTime()

        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

        for (i in 0 until min(possibleSlotMachines.size, SearchHolder.PAGE_CHUNK)) {
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
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totalPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        result.add(ActionRow.of(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS)))

        return result
    }
}