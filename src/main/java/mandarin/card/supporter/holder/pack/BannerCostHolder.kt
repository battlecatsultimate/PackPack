package mandarin.card.supporter.holder.pack

import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.filter.BannerFilter
import mandarin.card.supporter.holder.modal.CardCostAmountHolder
import mandarin.card.supporter.pack.BannerCardCost
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

class BannerCostHolder(author: Message, channelID: String, private val message: Message, private val pack: CardPack, private val cardCost: BannerCardCost, private val new: Boolean) : ComponentHolder(author, channelID, message.id) {

    private var page = 0

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        when(event.componentId) {
            "banner" -> {
                if (event !is StringSelectInteractionEvent)
                    return

                val banner = BannerFilter.Banner.valueOf(event.values[0])

                cardCost.banner = banner

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                applyResult(event)
            }
            "amount" -> {
                val input = TextInput.create("amount", "Amount", TextInputStyle.SHORT)
                    .setPlaceholder("Decide amount of required cards")
                    .setRequired(true)
                    .build()

                val modal = Modal.create("amount", "Required Cards Amount")
                    .addActionRow(input)
                    .build()

                event.replyModal(modal).queue()

                connectTo(CardCostAmountHolder(authorMessage, channelID, message.id, pack, cardCost))
            }
            "create" -> {
                pack.cost.cardsCosts.add(cardCost)

                if (pack in CardData.cardPacks) {
                    CardBot.saveCardData()
                }

                event.deferReply()
                    .setContent("Successfully added card cost to this card pack!")
                    .setEphemeral(true)
                    .queue()

                expired = true

                parent?.goBack()
            }
            "back" -> {
                if (new) {
                    registerPopUp(
                        event,
                        "Are you sure you want to cancel creating card cost and go back? This can't be undone",
                        LangID.EN
                    )

                    connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                        expired = true

                        e.deferEdit().queue()

                        parent?.goBack()
                    }, LangID.EN))
                } else {
                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    expired = true

                    event.deferEdit().queue()

                    goBack()
                }
            }
            "delete" -> {
                registerPopUp(
                    event,
                    "Are you sure you want to delete card cost? This can't be undone",
                    LangID.EN
                )

                connectTo(ConfirmPopUpHolder(authorMessage, channelID, message, { e ->
                    pack.cost.cardsCosts.remove(cardCost)

                    if (pack in CardData.cardPacks) {
                        CardBot.saveCardData()
                    }

                    e.deferReply()
                        .setContent("Successfully deleted card cost!")
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

    override fun onConnected(event: GenericComponentInteractionCreateEvent) {
        applyResult(event)
    }

    override fun onBack(child: Holder) {
        super.onBack(child)

        applyResult()
    }

    private fun applyResult() {
        message.editMessage(getContents())
            .setComponents(getComponents())
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun applyResult(event: GenericComponentInteractionCreateEvent) {
        event.deferEdit()
            .setContent(getContents())
            .setComponents(getComponents())
            .mentionRepliedUser(false)
            .queue()
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        val options = ArrayList<SelectOption>()

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

            options.add(SelectOption.of(bannerName, BannerFilter.pureBanners[i].name))
        }

        result.add(
            ActionRow.of(
                StringSelectMenu.create("banner")
                    .addOptions(options)
                    .setPlaceholder("Select card type to enable/disable as cost")
                    .build()
            )
        )

        val totalPage = ceil(BannerFilter.pureBanners.size * 1.0 / SearchHolder.PAGE_CHUNK)

        val buttons = ArrayList<Button>()

        if (totalPage > 10) {
            buttons.add(Button.secondary("prev10", "Previous 10 Pages").withEmoji(EmojiStore.TWO_PREVIOUS).withDisabled(page - 10 < 0))
        }

        buttons.add(Button.secondary("prev", "Previous Page").withEmoji(EmojiStore.PREVIOUS).withDisabled(page - 1 < 0))

        buttons.add(Button.secondary("next", "Next Page").withEmoji(EmojiStore.NEXT).withDisabled(page + 1 >= totalPage))

        if (totalPage > 10) {
            buttons.add(Button.secondary("next10", "Next 10 Pages").withEmoji(EmojiStore.TWO_NEXT).withDisabled(page + 10 >= totalPage))
        }

        result.add(ActionRow.of(buttons))

        result.add(ActionRow.of(Button.secondary("amount", "Set Amount of Cards")))

        if (new) {
            result.add(
                ActionRow.of(
                    Button.success("create", "Create").withDisabled(cardCost.isInvalid()),
                    Button.danger("back", "Go Back")
                )
            )
        } else {
            result.add(
                ActionRow.of(
                    Button.secondary("back", "Go Back"),
                    Button.danger("delete", "Delete Cost")
                )
            )
        }

        return result
    }

    private fun getContents() : String {
        val builder = StringBuilder("Required amount : ")
            .append(cardCost.amount)
            .append("\n\n")

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

            val checkSymbol = if (BannerFilter.pureBanners[i] == cardCost.banner) {
                EmojiStore.SWITCHON.formatted
            } else {
                EmojiStore.SWITCHOFF.formatted
            }

            builder.append(bannerName).append(" : ").append(checkSymbol).append("\n")
        }

        builder.append("\nSelect banner to disable/enable")

        if (cardCost.isInvalid()) {
            builder.append("\n\n**Warning : This card cost is invalid because it requires 0 card, or it doesn't have any banner required**")
        }

        return builder.toString()
    }
}