package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ServerData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.utils.FileUpload
import java.nio.file.Files

class EjectInventory : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid")) {
            return
        }

        val id = getUserID(loader.content.split(" "))

        if (id == -1L) {
            replyToMessageSafely(loader.channel, "You have provided invalid ID or no ID", loader.message) { a -> a }

            return
        }

        val inventory = CardData.inventories[id]

        if (inventory == null) {
            replyToMessageSafely(loader.channel, "The user <@$id> ($id) didn't have inventory yet", loader.message) { a -> a }

            return
        }

        replyToMessageSafely(loader.channel, "Are you sure you want to eject inventory of <@$id> ($id)? This cannot be undone", loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(u.id, ConfirmButtonHolder(loader.message, u.id, loader.channel.id, msg, CommonStatic.Lang.Locale.EN) {
                val file = inventory.extractAsFile()

                loader.client.retrieveUserById(StaticStore.MANDARIN_SMELL)
                    .flatMap { u -> u.openPrivateChannel() }
                    .flatMap { ch ->
                        ch.sendMessage("Inventory of <@$id> ($id)")
                            .setFiles(FileUpload.fromData(file, "inventory.json"))
                    }.queue {
                        if (!CardBot.test) {
                            loader.client.retrieveUserById(ServerData.get("gid"))
                                .flatMap { u -> u.openPrivateChannel() }
                                .flatMap { ch ->
                                    ch.sendMessage("Inventory of <@$id> ($id)")
                                        .setFiles(FileUpload.fromData(file, "inventory.json"))
                                }.queue {
                                    Files.deleteIfExists(file.toPath())
                                }
                        }
                    }

                CardData.inventories.remove(id)

                CardBot.saveCardData()

                replyToMessageSafely(loader.channel, "Successfully ejected inventory data from <@$id> ($id)!", loader.message) { a -> a }
            })
        }
    }

    private fun getUserID(contents: List<String>) : Long {
        return StaticStore.safeParseLong(contents.map { s -> s.replace("<@", "").replace(">", "") }.find { s -> StaticStore.isNumeric(s) } ?: "-1")
    }
}