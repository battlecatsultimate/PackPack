package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.skin.SkinCardSelectHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class ManageCardSkin : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m))
            return

        val cards = CardData.cards.sortedWith(CardComparator())

        replyToMessageSafely(loader.channel, getContents(cards), loader.message, { a -> a.setComponents(getComponents(cards)) }) { msg ->
            StaticStore.putHolder(m.id, SkinCardSelectHolder(loader.message, m.id, loader.channel.id, msg))
        }
    }

    private fun getContents(cards: List<Card>) : String {
        val builder = StringBuilder("Select card to manage skin of it\n\n```md\n")

        for (i in 0 until min(cards.size, SearchHolder.PAGE_CHUNK)) {
            builder.append(i + 1).append(". ").append(cards[i].simpleCardInfo()).append("\n")
        }

        builder.append("```")

        return builder.toString()
    }

    private fun getComponents(cards: List<Card>) : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val tierCategoryElements = ArrayList<SelectOption>()

        tierCategoryElements.add(SelectOption.of("All", "all"))

        CardData.tierCategoryText.forEachIndexed { index, text ->
            val emoji = EmojiStore.getCardEmoji(
                when (index) {
                    0 -> null
                    1 -> CardPack.CardType.T1
                    2 -> CardPack.CardType.T2
                    3 -> CardPack.CardType.T3
                    4 -> CardPack.CardType.T4
                    else -> throw IllegalStateException("E/CardModifyHolder::assignComponents - Invalid tier index $index")
                }
            )

            tierCategoryElements.add(SelectOption.of(text, "tier${index}").withEmoji(emoji))
        }

        val tierCategory = StringSelectMenu.create("tier")
            .addOptions(tierCategoryElements)
            .setPlaceholder("Filter Cards by Tiers")

        result.add(ActionRow.of(tierCategory.build()))

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(true))
        bannerCategoryElements.addAll(CardData.banners.filter { b -> b.category }.map { SelectOption.of(it.name, CardData.banners.indexOf(it).toString()) })

        if (bannerCategoryElements.size > 1) {
            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            result.add(ActionRow.of(bannerCategory.build()))
        }

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in 0 until min(dataSize, SearchHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card To See"
            )
            .setDisabled(cards.isEmpty())
            .build()

        result.add(ActionRow.of(cardCategory))

        var totPage = dataSize / SearchHolder.PAGE_CHUNK

        if (dataSize % SearchHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > SearchHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            if (totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))
            }

            result.add(ActionRow.of(buttons))
        }

        val confirmButtons = ArrayList<Button>()

        confirmButtons.add(Button.danger("close", "Close").withEmoji(EmojiStore.CROSS))

        result.add(ActionRow.of(confirmButtons))

        return result
    }
}