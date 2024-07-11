package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.util.unit.Form;
import common.util.unit.Level;
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
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class FormButtonHolder extends ComponentHolder {
    private final ConfigHolder config;
    private final Form f;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final boolean compact;
    private final Level lv;
    private final boolean treasure;
    private final TreasureHolder t;

    public FormButtonHolder(Form f, @Nonnull Message author, @Nonnull Message msg, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, boolean compact, boolean treasure, TreasureHolder t, Level lv, CommonStatic.Lang.Locale lang, @Nonnull String channelID) {
        super(author, channelID, msg, lang);

        this.config = config;
        this.f = f;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.compact = compact;
        this.treasure = treasure;
        this.t = t;
        this.lv = lv;

        StaticStore.executorHandler.postDelayed(FIVE_MIN, () -> {
            if(expired)
                return;

            expired = true;

            StaticStore.removeHolder(author.getAuthor().getId(), FormButtonHolder.this);

            expire(userID);
        });
    }

    @Override
    public void onEvent(@NotNull GenericComponentInteractionCreateEvent event) {
        message.delete().queue();

        MessageChannel ch = event.getMessageChannel();

        if(event.getComponentId().equals("talent")) {
            if(f.du.getPCoin() == null)
                return;

            try {
                EntityHandler.showTalentEmbed(ch, getAuthorMessage(), f, isFrame, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show talent embed on button click");
            }
        } else if (event.getComponentId().equals("dps")) {
            try {
                EntityHandler.showFormDPS(ch, getAuthorMessage(), f, t, lv, config, talent, treasure, lang);
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
                EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, treasure, t, lang, false, compact, msg -> { });
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show unit embed on button click");
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        ArrayList<Button> buttons = new ArrayList<>();

        for(Button button : message.getButtons()) {
            if(button.getStyle().getKey() == ButtonStyle.LINK.getKey()) {
                buttons.add(button);
            } else if(!compact) {
                buttons.add(button.asDisabled());
            }
        }

        if(buttons.isEmpty()) {
            message.editMessageComponents().mentionRepliedUser(false).queue(null, e -> {});
        } else {
            message.editMessageComponents(ActionRow.of(buttons)).mentionRepliedUser(false).queue(null, e -> {});
        }

        expired = true;
    }
}
