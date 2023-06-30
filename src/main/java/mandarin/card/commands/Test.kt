package mandarin.card.commands

import mandarin.card.supporter.transaction.TatsuHandler
import mandarin.packpack.commands.Command
import mandarin.packpack.supporter.lang.LangID
import net.dv8tion.jda.api.events.message.GenericMessageEvent
import java.lang.Exception

class Test : Command(LangID.EN, true) {
    override fun doSomething(event: GenericMessageEvent?) {
        val ch = getChannel(event) ?: return
        val g = getGuild(event) ?: return
        val m = getMember(event) ?: return

        ch.sendMessage("Trying to removed 10 cf from you").queue()

        try {
            TatsuHandler.modifyPoints(g.idLong, m.idLong, 10, TatsuHandler.Action.REMOVE, false)

            ch.sendMessage("Removed 10 cf").queue()
        } catch (e: Exception) {
            ch.sendMessage(e.message ?: "Error").queue()
        }
    }
}