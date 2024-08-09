package mandarin.card.commands

import common.CommonStatic
import mandarin.card.CardBot
import mandarin.card.supporter.TradingSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.server.CommandLoader
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Confirm(private val session: TradingSession) : Command(CommonStatic.Lang.Locale.EN, true) {
    override fun doSomething(loader: CommandLoader) {
        val ch = loader.channel
        val m = loader.member
        val g = loader.guild

        if (session.suggestion.any { s -> !s.touched }) {
            ch.sendMessage("Both user must suggest at least anything. Please call `${CardBot.globalPrefix}suggest` to suggest trade. It's fine to suggest nothing, but users must clearly indicate it via this command still")
                .queue()

            return
        }

        session.needApproval(g, {
            replyToMessageSafely(ch, "Please wait for manager or mod to approve this session!", loader.message) { a -> a }
        }, {
            val index = session.member.indexOf(m.idLong)

            if (index == -1)
                return@needApproval

            replyToMessageSafely(ch, "Are you sure you want to confirm this trading? Once trade is done, it cannot be undone", loader.message, { a ->
                val components = ArrayList<Button>()

                components.add(Button.success("confirm", "Confirm"))
                components.add(Button.danger("cancel", "Cancel"))

                a.setActionRow(components)
            }, { msg ->
                StaticStore.putHolder(m.id, ConfirmButtonHolder(loader.message, msg, ch.id, CommonStatic.Lang.Locale.EN, true) {
                    session.agreed[index] = true

                    val opposite = (2 - index) / 2

                    if (session.agreed[opposite]) {
                        ch.sendMessage("Both ${m.asMention} and <@${session.member[opposite]}> confirmed their trading")
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .setComponents()
                            .queue()

                        if (!session.validate(ch)) {
                            ch.sendMessage("It seems trading has been failed. Please check the reason above, and edit each other's suggestion to fix it").queue()

                            return@ConfirmButtonHolder
                        }

                        session.trade()

                        session.close(ch)

                        ch.sendMessage("Trading has been done, please check each other's inventory to check if trading has been done successfully. If you have suggested cf, please keep in mind that cat food transferring may take time").queue()
                    } else {
                        ch.sendMessage("${m.asMention} confirmed their trading, waiting for other's confirmation...")
                            .mentionRepliedUser(false)
                            .setAllowedMentions(ArrayList())
                            .setComponents()
                            .queue()

                        CardBot.saveCardData()
                    }
                })
            })
        })
    }
}