package mandarin.card.supporter.holder

import common.CommonStatic
import mandarin.card.supporter.card.Banner
import mandarin.packpack.supporter.server.holder.component.ComponentHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent

class BannerEditHolder(author: Message, userID: String, channelID: String, message: Message, private val banner: Banner) : ComponentHolder(author, userID, channelID, message, CommonStatic.Lang.Locale.EN) {
    override fun onEvent(event: GenericComponentInteractionCreateEvent) {
        TODO("Not yet implemented")
    }

    override fun clean() {
        TODO("Not yet implemented")
    }

    override fun onExpire() {
        TODO("Not yet implemented")
    }
}