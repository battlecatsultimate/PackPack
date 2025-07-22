package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.SalvageTierSelectHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class Salvage : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = if (loader.hasGuild()) {
            loader.member
        } else {
            val u = loader.user
            val g = loader.client.getGuildById(CardData.guild) ?: return

            val retriever = AtomicReference<Member>(null)
            val countdown = CountDownLatch(1)

            g.retrieveMember(u).queue({ m ->
                retriever.set(m)

                countdown.countDown()
            }) { e ->
                StaticStore.logger.uploadErrorLog(e, "E/Craft::doSomething - Failed to retrieve member data from user ID ${u.idLong}")

                countdown.countDown()
            }

            countdown.await()

            retriever.get() ?: return
        }

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        replyToMessageSafely(ch, "Select tier of the cards that will be salvaged", loader.message, { a ->
            a.setComponents(assignComponents())
        }, { message ->
            StaticStore.putHolder(m.id, SalvageTierSelectHolder(loader.message, m.id, ch.id, message))
        })
    }

    private fun assignComponents() : List<ActionRow> {
        val result = ArrayList<ActionRow>()

        val list = StringSelectMenu.create("tier")

        list.placeholder = "Select tier"

        list.addOptions(
            SelectOption.of("Tier 1 [Common]", "t1").withDescription("${CardData.SalvageMode.T1.cost} shard per card"),
            SelectOption.of("Regular Tier 2 [Uncommon]", "t2").withDescription("${CardData.SalvageMode.T2.cost} shards per card"),
            SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonalT2").withDescription("${CardData.SalvageMode.SEASONAL.cost} shards per card"),
            SelectOption.of("Collaboration Tier 2 [Uncommon]", "collaborationT2").withDescription("${CardData.SalvageMode.COLLAB.cost} shards per card"),
            SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3").withDescription("${CardData.SalvageMode.T3.cost} shards per card"),
            SelectOption.of("Tier 4 [Legend Rare]", "t4").withDescription("${CardData.SalvageMode.T4.cost} shards per card")
        )

        result.add(ActionRow.of(list.build()))

        result.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return result
    }
}