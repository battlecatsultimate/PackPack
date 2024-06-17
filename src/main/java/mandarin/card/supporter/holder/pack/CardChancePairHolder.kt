package mandarin.card.supporter.holder.pack

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.modal.CardChanceHolder
import mandarin.card.supporter.pack.CardChancePair
import mandarin.card.supporter.pack.CardChancePairList
import mandarin.card.supporter.pack.CardPack
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import mandarin.packpack.supporter.server.holder.component.ConfirmPopUpHolder
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import kotlin.math.ceil
import kotlin.math.min

class CardChancePairHolder(
    author: Message, channelID: String,
    private val message: Message,
    private val pack: CardPack,
    private val cardChancePairList: CardChancePairList,
    private val pair: CardChancePair,
    private val new: Boolean
) : ComponentHolder(author, channelID, message.id) {
    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "chance" -> {
                val input = TextInput.create("chance", "Chance", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setPlaceholder("Decide chance (Percentage)")
                    .build()

                val modal = Modal.create("chance", "Card Chance Adjustment")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardChanceHolder(authorMessage, channelID, message.id, pack, pair))
            }
            "tier" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedTier = CardPack.CardType.valueOf(event.values[0])

                if (selectedTier in pair.cardGroup.types) {
                    pair.cardGroup.types.remove(selectedTier)
                } else {
                    pair.cardGroup.types.add(selectedTier)
                }

                pair.cardGroup.types.sortBy { t -> t.ordinal }

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val selectedBanner = BannerFilter.Banner.valueOf(event.values[0])

                if (selectedBanner in pair.cardGroup.extra) {
                    pair.cardGroup.extra.remove(selectedBanner)
                } else {
                    pair.cardGroup.extra.add(selectedBanner)
                }

                pair.cardGroup.extra.sortBy { b -> b.ordinal }

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "create" -> {
                cardChancePairList.pairs.add(pair)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully create card/chance pair! Check result above")
                    .setEphemeral(true)
                    .queue()

                goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card/chance pair? This cannot be undone",
                        LangID.EN
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        e.deferEdit().queue()

                        goBack()
                    }, LangID.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete this card/chance pair? This cannot be undone",
                    LangID.EN
                )

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    cardChancePairList.pairs.remove(pair)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card/chance pair!")
                        .setEphemeral(true)
                        .queue()

                    goBack()
                }, LangID.EN))
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
        }
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    private fun applyResult() {
        message.editMessage(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContent())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getContent() : String {
        val builder = StringBuilder("## Card/Chance Pair Adjust Menu\n\nPack name : ")
            .append(pack.packName)
            .append("\n\nChance : ")
            .append(CardData.df.format(pair.chance))
            .append("%\n\n### Tier\n")

        CardPack.CardType.entries.forEach { tier ->
            val tierName = when(tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            val emoji = if (tier in pair.cardGroup.types)
                EmojiStore.SWITCHON
            else
                EmojiStore.SWITCHOFF

            builder.append("- ")
                .append(tierName)
                .append(" : ")
                .append(emoji.formatted)
                .append("\n")
        }

        builder.append("### Banner\n")

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, BannerFilter.pureBanners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = when(BannerFilter.pureBanners[i]) {
                BannerFilter.Banner.DarkHeroes -> "Dark Heroes"
                BannerFilter.Banner.DragonEmperors -> "Dragone Emperors"
                BannerFilter.Banner.Dynamites -> "Dynamites"
                BannerFilter.Banner.ElementalPixies -> "Elemental Pixies"
                BannerFilter.Banner.GalaxyGals -> "Galaxy Girls"
                BannerFilter.Banner.IronLegion -> "Iron Legion"
                BannerFilter.Banner.SengokuWargods -> "Sengoku Wargods"
                BannerFilter.Banner.TheNekolugaFamily -> "The Nekoluga Family"
                BannerFilter.Banner.UltraSouls -> "Ultra Souls"
                BannerFilter.Banner.GirlsAndMonsters -> "Girls And Monsters"
                BannerFilter.Banner.TheAlimighties -> "The Almighties"
                BannerFilter.Banner.EpicfestExclusives -> "Epicfest Exclusives"
                BannerFilter.Banner.UberfestExclusives -> "Uberfest Exclusives"
                BannerFilter.Banner.OtherExclusives -> "Other Exclusives"
                BannerFilter.Banner.BusterExclusives -> "Buster Exclusives"
                BannerFilter.Banner.Valentine -> "Valentine's Day"
                BannerFilter.Banner.Whiteday -> "White Day"
                BannerFilter.Banner.Easter -> "Easter"
                BannerFilter.Banner.JuneBride -> "June Bride"
                BannerFilter.Banner.SummerGals -> "Summer Gals"
                BannerFilter.Banner.Halloweens -> "Halloween"
                BannerFilter.Banner.XMas -> "X-Max"
                BannerFilter.Banner.Bikkuriman -> "Bikkuriman"
                BannerFilter.Banner.CrashFever -> "Crash Fever"
                BannerFilter.Banner.Fate -> "Fate Stay/Night"
                BannerFilter.Banner.Miku -> "Hatsune Miku"
                BannerFilter.Banner.MercStroia -> "Merc Storia"
                BannerFilter.Banner.Evangelion -> "Evangelion"
                BannerFilter.Banner.PowerPro -> "Power Pro Baseball"
                BannerFilter.Banner.Ranma -> "Ranma 1/2"
                BannerFilter.Banner.RiverCity -> "River City"
                BannerFilter.Banner.ShoumetsuToshi -> "Annihilated City"
                BannerFilter.Banner.StreetFighters -> "Street Fighters"
                BannerFilter.Banner.MolaSurvive -> "Survive! Mola Mola!"
                BannerFilter.Banner.MetalSlug -> "Metal Slug"
                BannerFilter.Banner.PrincessPunt -> "Princess Punt"
                BannerFilter.Banner.Collaboration,
                BannerFilter.Banner.Seasonal,
                BannerFilter.Banner.LegendRare ->  throw IllegalStateException("E/BannerCostHolder::getContents - Invalid banner ${BannerFilter.pureBanners[i]} found")

                BannerFilter.Banner.CheetahT1 -> "Tier 1 [Common]"
                BannerFilter.Banner.CheetahT2 -> "Tier 2 [Uncommon]"
                BannerFilter.Banner.CheetahT3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                BannerFilter.Banner.CheetahT4 -> "Tier 4 [Legend Rare]"
            }

            val emoji = if (BannerFilter.pureBanners[i] in pair.cardGroup.extra) {
                EmojiStore.SWITCHON.formatted
            } else {
                EmojiStore.SWITCHOFF.formatted
            }

            builder.append(bannerName).append(" : ").append(emoji).append("\n")
        }

        return builder.toString()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("chance", "Adjust Chance")))

        val tierOptions = ArrayList<SelectOption>()

        CardPack.CardType.entries.forEach { tier ->
            val tierName = when (tier) {
                CardPack.CardType.T1 -> "Tier 1 [Common]"
                CardPack.CardType.T2 -> "Tier 2 [Uncommon]"
                CardPack.CardType.REGULAR -> "Regular Tier 2 [Uncommon]"
                CardPack.CardType.SEASONAL -> "Seasonal Tier 2 [Uncommon]"
                CardPack.CardType.COLLABORATION -> "Collaboration Tier 2 [Uncommon]"
                CardPack.CardType.T3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                CardPack.CardType.T4 -> "Tier 4 [Legend Rare]"
            }

            tierOptions.add(SelectOption.of(tierName, tier.name))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("tier")
                    .addOptions(tierOptions)
                    .setPlaceholder("Select card type to enable/disable")
                    .build()
            )
        )

        val bannerOptions = ArrayList<SelectOption>()

        val size = min((page + 1) * SearchHolder.PAGE_CHUNK, BannerFilter.pureBanners.size)

        for (i in page * SearchHolder.PAGE_CHUNK until size) {
            val bannerName = when(BannerFilter.pureBanners[i]) {
                BannerFilter.Banner.DarkHeroes -> "Dark Heroes"
                BannerFilter.Banner.DragonEmperors -> "Dragone Emperors"
                BannerFilter.Banner.Dynamites -> "Dynamites"
                BannerFilter.Banner.ElementalPixies -> "Elemental Pixies"
                BannerFilter.Banner.GalaxyGals -> "Galaxy Girls"
                BannerFilter.Banner.IronLegion -> "Iron Legion"
                BannerFilter.Banner.SengokuWargods -> "Sengoku Wargods"
                BannerFilter.Banner.TheNekolugaFamily -> "The Nekoluga Family"
                BannerFilter.Banner.UltraSouls -> "Ultra Souls"
                BannerFilter.Banner.GirlsAndMonsters -> "Girls And Monsters"
                BannerFilter.Banner.TheAlimighties -> "The Almighties"
                BannerFilter.Banner.EpicfestExclusives -> "Epicfest Exclusives"
                BannerFilter.Banner.UberfestExclusives -> "Uberfest Exclusives"
                BannerFilter.Banner.OtherExclusives -> "Other Exclusives"
                BannerFilter.Banner.BusterExclusives -> "Buster Exclusives"
                BannerFilter.Banner.Valentine -> "Valentine's Day"
                BannerFilter.Banner.Whiteday -> "White Day"
                BannerFilter.Banner.Easter -> "Easter"
                BannerFilter.Banner.JuneBride -> "June Bride"
                BannerFilter.Banner.SummerGals -> "Summer Gals"
                BannerFilter.Banner.Halloweens -> "Halloween"
                BannerFilter.Banner.XMas -> "X-Max"
                BannerFilter.Banner.Bikkuriman -> "Bikkuriman"
                BannerFilter.Banner.CrashFever -> "Crash Fever"
                BannerFilter.Banner.Fate -> "Fate Stay/Night"
                BannerFilter.Banner.Miku -> "Hatsune Miku"
                BannerFilter.Banner.MercStroia -> "Merc Storia"
                BannerFilter.Banner.Evangelion -> "Evangelion"
                BannerFilter.Banner.PowerPro -> "Power Pro Baseball"
                BannerFilter.Banner.Ranma -> "Ranma 1/2"
                BannerFilter.Banner.RiverCity -> "River City"
                BannerFilter.Banner.ShoumetsuToshi -> "Annihilated City"
                BannerFilter.Banner.StreetFighters -> "Street Fighters"
                BannerFilter.Banner.MolaSurvive -> "Survive! Mola Mola!"
                BannerFilter.Banner.MetalSlug -> "Metal Slug"
                BannerFilter.Banner.PrincessPunt -> "Princess Punt"
                BannerFilter.Banner.Collaboration,
                BannerFilter.Banner.Seasonal,
                BannerFilter.Banner.LegendRare ->  throw IllegalStateException("E/BannerCostHolder::getContents - Invalid banner ${BannerFilter.pureBanners[i]} found")

                BannerFilter.Banner.CheetahT1 -> "Tier 1 [Common]"
                BannerFilter.Banner.CheetahT2 -> "Tier 2 [Uncommon]"
                BannerFilter.Banner.CheetahT3 -> "Tier 3 [Ultra Rare (Exclusives)]"
                BannerFilter.Banner.CheetahT4 -> "Tier 4 [Legend Rare]"
            }

            bannerOptions.add(SelectOption.of(bannerName, BannerFilter.pureBanners[i].name))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("banner")
                    .addOptions(bannerOptions)
                    .setPlaceholder("Select banner to enable/disable")
                    .build()
            )
        )

        val buttons = ArrayList<Button>()

        val totalPage = ceil(BannerFilter.pureBanners.size * 1.0 / SearchHolder.PAGE_CHUNK)

        if (totalPage > 10) {
            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
        }

        buttons.add(Button.secondary("prev", "Previous Page").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

        buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

        if (totalPage > 10) {
            buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
        }

        result.add(ActionRow.of(buttons))

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create"),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back"),
                    Button.danger("delete", "Delete")
                )
            )
        }

        return result
    }
}