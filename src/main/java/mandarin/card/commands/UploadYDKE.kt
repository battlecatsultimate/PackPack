package mandarin.card.commands

import common.CommonStatic
import mandarin.card.supporter.CardData
import mandarin.card.supporter.ServerData
import mandarin.card.supporter.holder.moderation.YDKEUploadHolder
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.EmojiStore
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay

class UploadYDKE : Command(CommonStatic.Lang.Locale.EN, false) {
    override fun doSomething(loader: CommandLoader) {
        val u = loader.user
        val m = CardData.userToMember(loader.client, u)

        if (m == null) {
            replyToMessageSafely(loader.channel, "Failed to load member data...", loader.message) { a -> a }

            return
        }

        if (u.id != StaticStore.MANDARIN_SMELL && u.id != ServerData.get("gid") && !CardData.isManager(m))
            return

        loader.channel.sendMessageComponents(getContainer())
            .useComponentsV2()
            .setMessageReference(loader.message)
            .setAllowedMentions(arrayListOf())
            .mentionRepliedUser(false)
            .queue({ msg ->
                StaticStore.putHolder(u.id, YDKEUploadHolder(loader.message, u.id, loader.channel.id, msg))
            }, { e -> StaticStore.logger.uploadErrorLog(e, "E/UploadYDKE::doSomething - Failed to send YDKE file type selection message")})
    }

    private fun getContainer() : Container {
        val components = ArrayList<ContainerChildComponent>()

        components.add(TextDisplay.of("Please select YDKE file type that you want to upload"))
        components.add(Separator.create(true, Separator.Spacing.LARGE))

        val options = ArrayList<SelectOption>()

        options.add(SelectOption.of("cards.cdb", "cards.cdb").withDescription("CDB data for general YDKE"))
        options.add(SelectOption.of("BCTC.cdb", "BCTC.cdb").withDescription("CDB data for BCTC custom cards"))
        options.add(SelectOption.of("Normal.lflist.conf", "Normal.lflist.conf").withDescription("White list data for normal case"))
        options.add(SelectOption.of("Tournament.lflist.conf", "Tournament.lflist.conf").withDescription("White list data for tournament case"))
        options.add(SelectOption.of("BCE.lflist.conf", "BCE.lflist.conf").withDescription("White list data for BCE case"))

        components.add(ActionRow.of(StringSelectMenu.create("file").addOptions(options).setPlaceholder("Select file type to upload").build()))
        components.add(ActionRow.of(Button.secondary("close", "Close").withEmoji(EmojiStore.CROSS)))

        return Container.of(components)
    }
}