package mandarin.card.commands

import mandarin.card.CardBot
import mandarin.card.supporter.TradingSession
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.StaticStore
import mandarin.packpack.supporter.lang.LangID
import mandarin.packpack.supporter.server.holder.component.ConfirmButtonHolder
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import net.dv8tion.jda.api.interactions.components.buttons.Button

class Confirm(private val session: TradingSession) : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val m = getMember(event) ?: return
        val g = getGuild(event) ?: return

        if (session.suggestion.any { s -> !s.touched }) {
            ch.sendMessage("Both user must suggest at least anything. Please call `${CardBot.globalPrefix}suggest` to suggest trade. It's fine to suggest nothing, but users must clearly indicate it via this command still")
                .queue()

            return
        }

        if (session.needApproval(g) && !session.approved) {
            replyToMessageSafely(ch, "Please wait for manager or mod to approve this session!", getMessage(event)) { a -> a }

            return
        }

        val index = session.member.indexOf(m.idLong)

        if (index == -1)
            return

        val msg = getRepliedMessageSafely(ch, "Are you sure you want to confirm this trading? Once trade is done, it cannot be undone", getMessage(event)) { a ->
            val components = ArrayList<Button>()

            components.add(Button.success("confirm", "Confirm"))
            components.add(Button.danger("cancel", "Cancel"))

            a.setActionRow(components)
        }

        StaticStore.putHolder(m.id, ConfirmButtonHolder(getMessage(event), msg, ch.id, {
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

                session.trade(ch, g.idLong)

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
        }, LangID.EN))
    }
}