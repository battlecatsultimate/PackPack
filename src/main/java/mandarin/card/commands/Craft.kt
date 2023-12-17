package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.CardCraftModeHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class Craft : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member

        if (CardBot.rollLocked && !CardData.hasAllPermission(m) && m.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val inventory = Inventory.getInventory(m.id)

        replyToMessageSafely(ch, "Select tier that you want to craft\n\n" +
                "You currently have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}", loader.message, { a ->
            a.setComponents(assignComponents())
        }) { message ->
            StaticStore.putHolder(m.id, CardCraftModeHolder(loader.message, ch.id, message))
        }
    }

    private fun assignComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val options = ArrayList<SelectOption>()

        options.add(SelectOption.of("Tier 2 [Uncommon]", "t2").withDescription("${CardData.CraftMode.T2.cost} shards required"))

        val seasonalCards = CardData.cards.filter { c -> c.unitID in BannerFilter.Banner.Seasonal.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }

        if (seasonalCards.isNotEmpty()) {
            options.add(SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonal").withDescription("${CardData.CraftMode.SEASONAL.cost} shards required"))
        }

        val collaborationCards = CardData.cards.filter { c -> c.unitID in BannerFilter.Banner.Collaboration.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.unitID in CardData.bannerData[a.tier.ordinal][a.banner] } }

        if (collaborationCards.isNotEmpty()) {
            options.add(SelectOption.of("Collaboration Tier 2 [Uncommon]", "collab").withDescription("${CardData.CraftMode.COLLAB.cost} shards required"))
        }

        options.add(SelectOption.of("Tier 3 [Ultra Rare (Exclusives)]", "t3").withDescription("${CardData.CraftMode.T3.cost} shards required"))
        options.add(SelectOption.of("Tier 4 [Legend Rare]", "t4").withDescription("${CardData.CraftMode.T4.cost} shards required"))

        rows.add(ActionRow.of(StringSelectMenu.create("tier").addOptions(options).build()))

        rows.add(ActionRow.of(Button.danger("cancel", "Cancel")))

        return rows
    }
}