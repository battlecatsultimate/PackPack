package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.holder.CardCraftModeHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class Craft : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
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

        val m = retriever.get() ?: return

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val inventory = Inventory.getInventory(m.idLong)

        replyToMessageSafely(ch, "Select tier that you want to craft\n\n" +
                "You currently have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}", loader.message, { a ->
            a.setComponents(assignComponents())
        }) { message ->
            StaticStore.putHolder(m.id, CardCraftModeHolder(loader.message, m.id, ch.id, message))
        }
    }

    private fun assignComponents() : List<MessageTopLevelComponent> {
        val rows = ArrayList<ActionRow>()

        val options = ArrayList<SelectOption>()

        options.add(
            SelectOption.of("Tier 2 [Uncommon]", "t2")
                .withDescription("${CardData.CraftMode.T2.cost} shards required")
                .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T2))
        )

        val seasonalCards = CardData.cards.filter { c -> c.cardType == Card.CardType.SEASONAL }.filter { c -> c.banner.any { b -> b in CardData.activatedBanners } }

        if (seasonalCards.isNotEmpty()) {
            options.add(
                SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonal")
                    .withDescription("${CardData.CraftMode.SEASONAL.cost} shards required")
                    .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL))
            )
        }

        val collaborationCards = CardData.cards.filter { c -> c.cardType == Card.CardType.COLLABORATION }.filter { c -> c.banner.any { b -> b in CardData.activatedBanners } }

        if (collaborationCards.isNotEmpty()) {
            options.add(
                SelectOption.of("Collaboration Tier 2 [Uncommon]", "collab")
                    .withDescription("${CardData.CraftMode.COLLAB.cost} shards required")
                    .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.COLLABORATION))
            )
        }

        options.add(
            SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3")
                .withDescription("${CardData.CraftMode.T3.cost} shards required")
                .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T3))
        )

        options.add(
            SelectOption.of("Tier 4 [Legend Rare]", "t4")
                .withDescription("${CardData.CraftMode.T4.cost} shards required")
                .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T4))
        )

        rows.add(ActionRow.of(StringSelectMenu.create("tier").addOptions(options).build()))

        rows.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return rows
    }
}