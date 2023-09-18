package mandarin.card.commands

import mandarin.card.CardBot
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

        if (CardBot.rollLocked && !CardData.isManager(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

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

        val largeDesc = if ((CardData.cooldown[member.id]?.get(CardData.LARGE) ?: 0) - CardData.getUnixEpochTime() > 0) {
            "Cooldown Left : ${CardData.convertMillisecondsToText((CardData.cooldown[member.id]?.get(CardData.LARGE) ?: 0) - CardData.getUnixEpochTime())}"
        } else {
            "8 Common + 1 Common/Uncommon + 1 Uncommon/Ultra Rare"
        }

        packOptions.add(SelectOption.of("Large Card Pack [10k cf]", "large").withDescription(largeDesc))

        val smallDesc = if ((CardData.cooldown[member.id]?.get(CardData.SMALL) ?: 0) - CardData.getUnixEpochTime() > 0) {
            "Cooldown Left : ${CardData.convertMillisecondsToText((CardData.cooldown[member.id]?.get(CardData.SMALL) ?: 0) - CardData.getUnixEpochTime())}"
        } else {
            "4 Common + 1 Common/Uncommon"
        }

        packOptions.add(SelectOption.of("Small Card Pack [5k cf]", "small").withDescription(smallDesc))

        val premiumDesc = if ((CardData.cooldown[member.id]?.get(CardData.PREMIUM) ?: 0) - CardData.getUnixEpochTime() > 0) {
            "Cooldown Left : ${CardData.convertMillisecondsToText((CardData.cooldown[member.id]?.get(CardData.PREMIUM) ?: 0) - CardData.getUnixEpochTime())}"
        } else {
            "5 Common/Ultra Rare/Legend Rare"
        }

        packOptions.add(SelectOption.of("Premium Card Pack [5 Tier 2 Cards]", "premium").withDescription(premiumDesc))

        val packs = StringSelectMenu.create("pack")
            .addOptions(packOptions)
            .setPlaceholder("Select pack here")
            .build()

        result.add(ActionRow.of(packs))

        return result
    }
}