package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Card
import mandarin.card.supporter.card.CardComparator
import mandarin.card.supporter.holder.moderation.CardActivateHolder
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import kotlin.math.min

class ActivateCard : Command(CommonStatic.Lang.Locale.EN, true) {
    private val cards = ArrayList<Card>(CardData.cards.sortedWith(CardComparator()))

    private var tier = CardData.Tier.NONE
    private var banner = Banner.NONE

    override fun doSomething(loader: CommandLoader) {
        if (loader.member.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(loader.member))
            return

        replyToMessageSafely(loader.channel, getContents(), loader.message, { a -> a.setComponents(getComponents()) }) { msg ->
            StaticStore.putHolder(loader.member.id, CardActivateHolder(loader.message, loader.member.id, loader.channel.id, msg))
        }
    }

    private fun getComponents() : List<LayoutComponent> {
        val rows = ArrayList<ActionRow>()

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

        if (tier == CardData.Tier.NONE) {
            tierCategory.setDefaultOptions(tierCategoryElements[0])
        } else {
            tierCategory.setDefaultOptions(tierCategoryElements[tier.ordinal + 1])
        }

        rows.add(ActionRow.of(tierCategory.build()))

        val bannerCategoryElements = ArrayList<SelectOption>()

        bannerCategoryElements.add(SelectOption.of("All", "all").withDefault(banner === Banner.NONE))
        val bannerList = if (tier != CardData.Tier.NONE) {
            CardData.banners.filter { b -> b.category && CardData.cards.any { c -> b in c.banner && c.tier == tier } }
        } else {
            CardData.banners.filter { b -> b.category }
        }

        bannerCategoryElements.addAll(bannerList.map { SelectOption.of(it.name, CardData.banners.indexOf(it).toString()).withDefault(it === banner) })

        if (bannerCategoryElements.size > 1) {
            val bannerCategory = StringSelectMenu.create("category")
                .addOptions(bannerCategoryElements)
                .setPlaceholder("Filter Cards by Banners")

            rows.add(ActionRow.of(bannerCategory.build()))
        }

        val dataSize = cards.size

        val cardCategoryElements = ArrayList<SelectOption>()

        if (cards.isEmpty()) {
            cardCategoryElements.add(SelectOption.of("a", "-1"))
        } else {
            for(i in 0 until min(dataSize, CardActivateHolder.PAGE_CHUNK)) {
                cardCategoryElements.add(SelectOption.of(cards[i].simpleCardInfo(), i.toString()))
            }
        }

        val cardCategory = StringSelectMenu.create("card")
            .addOptions(cardCategoryElements)
            .setPlaceholder(
                if (cards.isEmpty())
                    "No Cards To Select"
                else
                    "Select Card"
            )
            .setDisabled(cards.isEmpty())
            .build()

        rows.add(ActionRow.of(cardCategory))

        var totPage = dataSize / CardActivateHolder.PAGE_CHUNK

        if (dataSize % CardActivateHolder.PAGE_CHUNK != 0)
            totPage++

        if (dataSize > CardActivateHolder.PAGE_CHUNK) {
            val buttons = ArrayList<Button>()

            if(totPage > 10) {
                buttons.add(Button.of(ButtonStyle.SECONDARY, "prev10", "Previous 10 Pages", EmojiStore.TWO_PREVIOUS).asDisabled())
            }

            buttons.add(Button.of(ButtonStyle.SECONDARY, "prev", "Previous Pages", EmojiStore.PREVIOUS).asDisabled())

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next", "Next Page", EmojiStore.NEXT))

            buttons.add(Button.of(ButtonStyle.SECONDARY, "next10", "Next 10 Pages", EmojiStore.TWO_NEXT))

            rows.add(ActionRow.of(buttons))
        }

        rows.add(ActionRow.of(Button.primary("close", "Close")))

        return rows
    }

    private fun getContents() : String {
        val activate = EmojiStore.SWITCHON.formatted
        val deactivate = EmojiStore.SWITCHOFF.formatted

        val builder = StringBuilder(
            "Please select cards to toggle activation. Cards which have $activate emoji are " +
            "activated, and users will be able to get them via rolling the pack or the slot " +
            "machine. Cards which have $deactivate emoji are deactivated. Users can own these" +
            "or trade them, but they can't obtain more from rolling the pack or the slot machine"
        )

        builder.append("\n\n### Cards List\n\n")

        if (cards.isNotEmpty()) {
            for (i in 0 until min(CardActivateHolder.PAGE_CHUNK, cards.size)) {
                builder.append("${i + 1}. ${cards[i].cardInfo()}")

                if (cards[i].activated)
                    builder.append(" ").append(activate)
                else
                    builder.append(" ").append(deactivate)

                builder.append("\n")
            }
        } else {
            builder.append("No Cards Found")
        }

        return builder.toString()
    }
}