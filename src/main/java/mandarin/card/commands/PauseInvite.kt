package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.CardData
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder

class PauseInvite : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val m = loader.member

        if (m.id != StaticStore.MANDARIN_SMELL && !CardData.isMod(m))
            return

        val g = loader.guild

        val message = if (CardBot.inviteLocked) {
            "Are you sure you want to automatically enable invite links?"
        } else {
            "Are you sure you want to automatically disable (pause) invite links?"
        }

        replyToMessageSafely(loader.channel, message, loader.message, { a -> registerConfirmButtons(a, CommonStatic.Lang.Locale.EN) }) { msg ->
            StaticStore.putHolder(loader.member.id, ConfirmButtonHolder(loader.message, msg, loader.channel.id, CommonStatic.Lang.Locale.EN) {
                replyToMessageSafely(loader.channel, if (CardBot.inviteLocked) "Enabling..." else "Disabling...", loader.message, { a -> a }) { m ->
                    g.manager.setInvitesDisabled(!CardBot.inviteLocked).queue( {
                        val alreadyLocked = CardBot.inviteLocked
                        CardBot.inviteLocked = !CardBot.inviteLocked

                        CardBot.saveCardData()

                        if (alreadyLocked) {
                            m.editMessage("Successfully enabled the invites links!")
                                .mentionRepliedUser(false)
                                .queue()
                        } else {
                            m.editMessage("Successfully disabled the invites links! Now bot will keep checking if invites are unlocked or not too")
                                .mentionRepliedUser(false)
                                .queue()
                        }
                    }) { e ->
                        StaticStore.logger.uploadErrorLog(e, "E/PauseInvite::doSomething - Failed to pause/unpause the server")
                    }
                }
            })
        }
    }
}