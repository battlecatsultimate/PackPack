package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.holder.moderation.CatFoodRateConfigHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class CatFoodRate : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val member = loader.member
        val ch = loader.channel

        if (!CardData.isManager(member) && member.id != StaticStore.MANDARIN_SMELL) {
            return
        }

        val timeText = if (CardData.catFoodCooldown > 0)
            "Every `" + CardData.convertMillisecondsToText(CardData.catFoodCooldown) + "`"
        else
            "Every message"

        replyToMessageSafely(ch, "Minimum Cat Food : ${EmojiStore.ABILITY["CF"]?.formatted} ${CardData.minimumCatFoods}\n" +
                "Maximum Cat Food : ${EmojiStore.ABILITY["CF"]?.formatted} ${CardData.maximumCatFoods}\n" +
                "\n" +
                "Cooldown : $timeText", loader.message, { a -> a.setComponents(getComponents()) }) { m ->
            StaticStore.putHolder(loader.member.id, CatFoodRateConfigHolder(loader.message, member.id, loader.channel.id, m))
        }
    }

    private fun getComponents() : List<LayoutComponent> {
        val result = ArrayList<LayoutComponent>()

        result.add(ActionRow.of(Button.secondary("cf", "Define Cat Foods").withEmoji(EmojiStore.ABILITY["CF"])))
        result.add(ActionRow.of(Button.secondary("cooldown", "Define Cooldown (in seconds)").withEmoji(Emoji.fromUnicode("⏰"))))
        result.add(ActionRow.of(Button.primary("confirm", "Confirm").withEmoji(Emoji.fromUnicode("✅"))))

        return result
    }
}