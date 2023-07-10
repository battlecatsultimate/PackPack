package mandarin.card.commands

import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.PackSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Roll : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return

        val msg = getRepliedMessageSafely(ch, "Please select the pack that you want to roll", getMessage(event)) { a ->
            a.setComponents(registerComponents(m))
        }

        val content = getContent(event).split(" ")

        val noImage = arrayOf("-s", "-simple", "-n", "-noimage").any { p -> p in content }

        StaticStore.putHolder(m.id, PackSelectHolder(getMessage(event), ch.id, msg, noImage))
    }

    private fun registerComponents(member: Member) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val packOptions = ArrayList<SelectOption>()

        val largeDesc = if ((CardData.cooldown[member.id]?.get(1) ?: 0) - CardData.getUnixEpochTime() > 0) {
            "Cooldown Left : ${CardData.convertMillisecondsToText((CardData.cooldown[member.id]?.get(1) ?: 0) - CardData.getUnixEpochTime())}"
        } else {
            "10k Cat Foods : 8 Common + 1 Uncommon + 1 Uncommon/Ultra Rare/Legend Rare"
        }

        packOptions.add(SelectOption.of("Large Card Pack", "large").withDescription(largeDesc))

        val smallDesc = if ((CardData.cooldown[member.id]?.get(0) ?: 0) - CardData.getUnixEpochTime() > 0) {
            "Cooldown Left : ${CardData.convertMillisecondsToText((CardData.cooldown[member.id]?.get(0) ?: 0) - CardData.getUnixEpochTime())}"
        } else {
            "5k Cat Foods : 4 Common + 1 Uncommon/Ultra Rare"
        }

        packOptions.add(SelectOption.of("Small Card Pack", "small").withDescription(smallDesc))

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

        return result
    }
}