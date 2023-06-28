package mandarin.card.supporter.holder

import mandarin.card.supporter.Card
import mandarin.card.supporter.filter.Filter
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent

class CardProcessHolder(author: Message, channelID: String, private val message: Message, private val filters: List<Filter>) : ComponentHolder(author, channelID, message.id) {
    private val selectedCards = ArrayList<Card>()

    override fun clean() {

    }

    override fun onExpire(id: String?) {

    }

    override fun onEvent(event: GenericComponentInteractionCreateEvent?) {

    }
}