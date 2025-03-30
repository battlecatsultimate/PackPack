package mandarin.card.supporter.holder.skin

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.card.Banner
import mandarin.card.supporter.card.Skin
import mandarin.card.supporter.pack.BannerCardCost
import mandarin.card.supporter.pack.CardCost
import mandarin.card.supporter.pack.CardPack
import mandarin.card.supporter.pack.SpecificCardCost
import mandarin.card.supporter.pack.TierCardCost
import mandarin.packpack.supporter.server.holder.Holder
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import java.util.concurrent.TimeUnit

class SkinCostTypeHolder(author: Message, userID: String, channelID: String, message: Message, private val skin: Skin) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    init {
        registerAutoExpiration(TimeUnit.HOURS.toMillis(1L))
    }

    override fun clean() {

    }

    override fun onExpire() {
        message.editMessage("Skin manager expired")
            .setComponents()
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        if (event.componentId == "type") {
            if (event !is StringSelectInteractionEvent)
                return

            val type = CardCost.CostType.valueOf(event.values[0])

            when(type) {
                CardCost.CostType.BANNER -> {
                    connectTo(event, SkinBannerCostHolder(authorMessage, userID, channelID, message, skin, BannerCardCost(Banner.fromName("Dark Heroes"), 0), true))
                }
                CardCost.CostType.TIER -> {
                    connectTo(event, SkinTierCostHolder(authorMessage, userID, channelID, message, skin, TierCardCost(CardPack.CardType.T1, 0), true))
                }
                CardCost.CostType.CARD -> {
                    connectTo(event, SkinSpecificCardCostHolder(authorMessage, userID, channelID, message, skin, SpecificCardCost(HashSet(), 1), true))
                }
            }
        } else if (event.componentId == "back") {
            if (skin in CardData.skins) {
                CardBot.saveCardData()
            }

            event.deferEdit().queue()

            goBack()
        }
    }

    override fun onConnected(event: IMessageEditCallback, parent: Holder) {
        val options = ArrayList<SelectOption>()

        for (type in CardCost.CostType.entries) {
            if (skin.cost.cardsCosts.isEmpty()) {
                when (type) {
                    CardCost.CostType.BANNER -> options.add(SelectOption.of("Banner", type.name))
                    CardCost.CostType.TIER -> options.add(SelectOption.of("Tier", type.name))
                    CardCost.CostType.CARD -> options.add(SelectOption.of("Card", type.name))
                }
            } else {
                when (type) {
                    CardCost.CostType.BANNER -> options.add(SelectOption.of("Banner", type.name))
                    CardCost.CostType.TIER -> options.add(SelectOption.of("Tier", type.name))
                    CardCost.CostType.CARD -> {}
                }
            }
        }

        event.deferEdit()
            .setContent("Select type of cost that you want to add")
            .setComponents(
                arrayListOf(
                    ActionRow.of(
                        StringSelectMenu.create("type")
                            .addOptions(options)
                            .setPlaceholder("Decide type of card cost")
                            .build()
                    ),
                    ActionRow.of(
                        Button.secondary("back", "Go Back")
                    )
                )
            )
            .setAllowedMentions(ArrayList())
            .mentionRepliedUser(false)
            .queue()
    }
}