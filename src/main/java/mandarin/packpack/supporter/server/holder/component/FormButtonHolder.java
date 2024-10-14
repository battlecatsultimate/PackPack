package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import mandarin.packpack.commands.bc.FormStat;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class FormButtonHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final Form f;

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
        MessageChannel ch = event.getMessageChannel();

        if(event.getComponentId().equals("talent")) {
            if(f.du.getPCoin() == null)
                return;

            try {
                EntityHandler.showTalentEmbed(event, hasAuthorMessage() ? getAuthorMessage() : null, f, configData.isFrame, true, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show talent embed on button click");
            }
        } else if (event.getComponentId().equals("dps")) {
            try {
                EntityHandler.showFormDPS(event, hasAuthorMessage() ? getAuthorMessage() : null, f, t, configData.lv, config, configData.talent, configData.treasure, true, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show DPS graph on buttone click");
            }
        } else {
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

            Form newForm = f.unit.forms[f.fid + diff];

            try {
                EntityHandler.showUnitEmb(newForm, event, hasAuthorMessage() ? getAuthorMessage() : null, config, false, t, configData, lang, false, true, msg -> { });
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show unit embed on button click");
            }
        }

        end(true);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        ArrayList<Button> buttons = new ArrayList<>();

        for(Button button : message.getButtons()) {
            if(button.getStyle().getKey() == ButtonStyle.LINK.getKey()) {
                buttons.add(button);
            } else if(!configData.compact) {
                buttons.add(button.asDisabled());
            }
        }

        if(buttons.isEmpty()) {
            message.editMessageComponents().mentionRepliedUser(false).queue(null, e -> {});
        } else {
            message.editMessageComponents(ActionRow.of(buttons)).mentionRepliedUser(false).queue(null, e -> {});
        }
    }
}
