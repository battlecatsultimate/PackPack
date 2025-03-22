package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.server.holder.Holder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ComponentHolder extends Holder {
    public ComponentHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, @Nonnull CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);
    }

    @Override
    public final STATUS handleEvent(Event event) {
        if(event instanceof GenericComponentInteractionCreateEvent componentEvent && canHandleEvent(componentEvent)) {
            onEvent(componentEvent);
        }

        return STATUS.FINISH;
    }

    @Override
    public final Type getType() {
        return Type.COMPONENT;
    }

    public abstract void onEvent(@Nonnull GenericComponentInteractionCreateEvent event);

    public int parseDataToInt(GenericComponentInteractionCreateEvent event) {
        if(!(event instanceof StringSelectInteractionEvent)) {
            throw new IllegalStateException("Event type isn't StringSelectInteractionEvent!");
        }

        return StaticStore.safeParseInt(((StringSelectInteractionEvent) event).getValues().getFirst());
    }

    private boolean canHandleEvent(GenericComponentInteractionCreateEvent event) {
        return event.getChannel().getId().equals(channelID)
                && event.getMessage().getId().equals(message.getId())
                && event.getUser().getId().equals(userID);
    }
}
