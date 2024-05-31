package mandarin.card.commands

import mandarin.card.supporter.ServerData
import mandarin.card.supporter.slot.SlotEmojiContainer
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.CommandLoader

class RegisterEmojiServer : Command(LangID.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && m.id != ServerData.get("gid"))
            return

        val contents = loader.content.trim().split(" ")

        if (contents.size < 2) {
            replyToMessageSafely(loader.channel, "You have to pass ID of the guild that will be registered!", loader.message) { a -> a }

            return
        }

        val id = contents[1]

        if (!StaticStore.isNumeric(id)) {
            replyToMessageSafely(loader.channel, "Guild ID must be numeric value!", loader.message) { a -> a }

            return
        }

        val g = loader.client.getGuildById(StaticStore.safeParseLong(id))

        if (g == null) {
            replyToMessageSafely(loader.channel, "Failed to find such guild! Maybe bot isn't in there, or you have passed invalid ID?", loader.message) { a -> a }

            return
        }

        if (g.idLong in SlotEmojiContainer.registeredServer) {
            replyToMessageSafely(loader.channel, "This guild is already registered as emoji guild!", loader.message) { a -> a }

            return
        }

        SlotEmojiContainer.registerServer(g)

        replyToMessageSafely(loader.channel, "Successfully registered the guild\n\nGuild Name : ${g.name}\nID : ${g.idLong}\nNumber of Emojis : ${g.emojis.size}", loader.message) { a -> a }
    }
}