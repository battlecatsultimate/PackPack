package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader

class Tax : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isManager(m)) {
            return
        }

        val contents = loader.content.split(" ")

        if (contents.size < 3) {
            replyToMessageSafely(loader.channel, "Format : `cd.tax [User] [Amount]`", loader.message) { a -> a }

            return
        }

        if (!contents[1].matches(Regex("(<@)?\\d+(>)?"))) {
            replyToMessageSafely(loader.channel, "Users must be either ID or mention!", loader.message) { a -> a }

            return
        }

        val id = StaticStore.safeParseLong(contents[1].replace(Regex("(<@|>)"), ""))

        if (!StaticStore.isNumeric(contents[2])) {
            replyToMessageSafely(loader.channel, "Amount must be numeric!", loader.message) { a -> a }

            return
        }

        val amount = StaticStore.safeParseLong(contents[2])

        CardData.taxedCatFoods[id] = (CardData.taxedCatFoods[id] ?: 0L) + amount

        replyToMessageSafely(loader.channel, "Yoink!", loader.message) { a -> a }
    }
}