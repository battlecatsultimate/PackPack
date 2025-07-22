package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
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

public class FormDPSButtonHolder extends ComponentHolder {
    private final Form f;
    private final TreasureHolder t;
    private final ConfigHolder config;
    private final FormStat.FormStatConfig configData;

    public FormDPSButtonHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, Form f, TreasureHolder t, ConfigHolder config, FormStat.FormStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.f = f;
        this.t = t;
        this.config = config;
        this.configData = configData;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        if (event.getComponentId().equals("back")) {
            goBack(event);
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        ArrayList<ActionRow> rows = new ArrayList<>();

        for (ActionRow row : message.getComponentTree().findAll(ActionRow.class)) {
            ArrayList<Button> expiredButtons = new ArrayList<>();

            for (Button button : row.getButtons()) {
                if (button.getStyle().getKey() == ButtonStyle.LINK.getKey()) {
                    expiredButtons.add(button);
                } else if (!configData.compact) {
                    expiredButtons.add(button.asDisabled());
                }
            }

            if (!expiredButtons.isEmpty()) {
                rows.add(ActionRow.of(expiredButtons));
            }
        }

        if(rows.isEmpty()) {
            message.editMessageComponents().mentionRepliedUser(false).queue(null, e -> {});
        } else {
            message.editMessageComponents(rows).mentionRepliedUser(false).queue(null, e -> {});
        }
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        EntityHandler.showFormDPS(event, hasAuthorMessage() ? getAuthorMessage() : null, f, t, configData.lv, config, configData.talent, configData.treasure, true, lang);
    }
}
