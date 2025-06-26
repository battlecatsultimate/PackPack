package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.stage.Stage;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StageLineupHolder extends ComponentHolder {
    private final Stage st;

    public StageLineupHolder(Stage st, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.st = st;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {

    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        EntityHandler.showFixedLineupData(st, st.preset, event, lang);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (event.getComponentId().equals("back")) {
            goBack(event);
        }
    }
}
