package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.Inventory
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu

class CardCraftModeHolder(author: Message, userID: String, channelID: String, message: Message) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    val inventory = Inventory.getInventory(author.author.idLong)

    init {
        registerAutoExpiration(FIVE_MIN)
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Craft expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "cancel" -> {
                event.deferEdit()
                    .setContent("Canceled craft")
                    .setComponents()
                    .setAllowedMentions(ArrayList())
                    .mentionRepliedUser(false)
                    .queue()
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedMode = when(event.values[0]) {
                    "t2" -> CardData.CraftMode.T2
                    "seasonal" -> CardData.CraftMode.SEASONAL
                    "collab" -> CardData.CraftMode.COLLAB
                    "t3" -> CardData.CraftMode.T3
                    else -> CardData.CraftMode.T4
                }

                connectTo(event, CardCraftAmountHolder(authorMessage, userID, channelID, message, selectedMode))
            }
        }
    }

    override fun onBack(event: IMessageEditCallback, child: Holder) {
        applyResult(event)
    }

    private fun applyResult(event: IMessageEditCallback) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContents() : String {
        return "Select tier that you want to craft\n\nYou currently have ${EmojiStore.ABILITY["SHARD"]?.formatted} ${inventory.platinumShard}"
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

        val options = ArrayList<SelectOption>()

        options.add(
            SelectOption.of("Tier 2 [Uncommon]", "t2")
                .withDescription("${CardData.CraftMode.T2.cost} shards required")
                .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.T2))
        )

        val seasonalCards = CardData.cards.filter { c -> c.id in BannerFilter.Banner.Seasonal.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.id in CardData.bannerData[a.tier.ordinal][a.banner] } }

        if (seasonalCards.isNotEmpty()) {
            options.add(
                SelectOption.of("Seasonal Tier 2 [Uncommon]", "seasonal")
                    .withDescription("${CardData.CraftMode.SEASONAL.cost} shards required")
                    .withEmoji(EmojiStore.getCardEmoji(CardPack.CardType.SEASONAL))
            )
        }

        val collaborationCards = CardData.cards.filter { c -> c.id in BannerFilter.Banner.Collaboration.getBannerData() }.filter { c -> CardData.activatedBanners.any { a -> c.id in CardData.bannerData[a.tier.ordinal][a.banner] } }

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