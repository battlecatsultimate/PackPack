package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class FormButtonHolder extends ComponentHolder {
    private final ConfigHolder config;
    private Form f;

    private final FormStat.FormStatConfig configData;
    private final TreasureHolder t;

    public FormButtonHolder(Form f, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, ConfigHolder config, TreasureHolder t, FormStat.FormStatConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.config = config;
        this.f = f;

        this.configData = configData;
        this.t = t;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "talent" -> connectTo(event, new FormTalentHolder(hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, f, configData, lang));
            case "dps" -> connectTo(event, new FormDPSHolder(hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, f, t, config, configData, lang));
            default -> {
                int diff = switch (event.getComponentId()) {
                    case "first" -> -3;
                    case "twoPre" -> -2;
                    case "pre" -> -1;
                    case "next" -> 1;
                    case "twoNext" -> 2;
                    case "final" -> 3;
                    default -> 0;
                };

                if(diff == 0) {
                    return;
                }

                if(f.fid + diff < 0)
                    return;

                if(f.unit == null)
                    return;

                f = f.unit.forms[f.fid + diff];

                try {
                    EntityHandler.showUnitEmb(f, event, hasAuthorMessage() ? getAuthorMessage() : null, config, f.unit.forms.length >= 3, t, configData, lang, true, true, msg -> { });
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show unit embed on button click");
                }
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        ArrayList<ActionRow> rows = new ArrayList<>();

        for (ActionRow row : message.getActionRows()) {
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
    public void onBack(@NotNull IMessageEditCallback event, @NotNull Holder child) throws Exception {
        EntityHandler.showUnitEmb(f, event, hasAuthorMessage() ? getAuthorMessage() : null, config, f.unit.forms.length >= 3, t, configData, lang, true, true, msg -> { });
    }
}
