package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Enemy;
import mandarin.packpack.commands.bc.EnemyStat;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class EnemyButtonHolder extends ComponentHolder {
    private final Enemy e;

    private final TreasureHolder t;
    private final EnemyStat.EnemyStatConfig configData;

    public EnemyButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, Enemy e, TreasureHolder t, EnemyStat.EnemyStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);
        this.e = e;

        this.t = t;
        this.configData = configData;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        if (event.getComponentId().equals("dps")) {
            connectTo(event, new EnemyDPSButtonHolder(hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, e, t, configData, lang));
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        ArrayList<Button> buttons = new ArrayList<>();

        for(Button button : message.getComponentTree().findAll(Button.class)) {
            if(button.getStyle().getKey() == ButtonStyle.LINK.getKey()) {
                buttons.add(button);
            } else if(!configData.isCompact) {
                buttons.add(button.asDisabled());
            }
        }

        if(buttons.isEmpty()) {
            message.editMessageComponents().mentionRepliedUser(false).queue(null, e -> {});
        } else {
            message.editMessageComponents(ActionRow.of(buttons)).mentionRepliedUser(false).queue(null, e -> {});
        }
    }

    @Override
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) throws Exception {
        EntityHandler.showEnemyEmb(e, event, hasAuthorMessage() ? getAuthorMessage() : null, t, configData, true, lang, msg -> {});
    }
}
