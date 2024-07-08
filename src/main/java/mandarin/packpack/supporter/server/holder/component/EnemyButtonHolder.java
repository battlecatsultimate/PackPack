package mandarin.packpack.supporter.server.holder.component;

import common.util.unit.Enemy;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class EnemyButtonHolder extends ComponentHolder {
    private final Message embed;
    private final int lang;
    private final Enemy e;

    private final int[] magnification;
    private final boolean compact;
    private final TreasureHolder t;

    public EnemyButtonHolder(Enemy e, @Nonnull Message author, @Nonnull Message msg, TreasureHolder t, int[] magnification, boolean compact, int lang, @Nonnull String channelID) {
        super(author, channelID, msg);

        this.embed = msg;
        this.lang = lang;
        this.e = e;

        this.magnification = magnification;
        this.t = t;
        this.compact = compact;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), EnemyButtonHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        embed.delete().queue();

        MessageChannel ch = event.getMessageChannel();

        if (event.getComponentId().equals("dps")) {
            try {
                EntityHandler.showEnemyDPS(ch, getAuthorMessage(), e, t, magnification[1], lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show DPS graph on buttone click");
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        ArrayList<Button> buttons = new ArrayList<>();

        for(Button button : embed.getButtons()) {
            if(button.getStyle().getKey() == ButtonStyle.LINK.getKey()) {
                buttons.add(button);
            } else if(!compact) {
                buttons.add(button.asDisabled());
            }
        }

        if(buttons.isEmpty()) {
            embed.editMessageComponents().mentionRepliedUser(false).queue(null, e -> {});
        } else {
            embed.editMessageComponents(ActionRow.of(buttons)).mentionRepliedUser(false).queue(null, e -> {});
        }

        expired = true;
    }
}
