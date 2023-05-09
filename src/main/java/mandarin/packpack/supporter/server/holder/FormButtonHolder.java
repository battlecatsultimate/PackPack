package mandarin.packpack.supporter.server.holder;

import common.util.unit.Form;
import common.util.unit.Level;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.holder.segment.InteractionHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class FormButtonHolder extends InteractionHolder {
    private final Message embed;
    private final ConfigHolder config;
    private final int lang;
    private final Form f;

    private final boolean isFrame;
    private final boolean talent;
    private final boolean extra;
    private final boolean compact;
    private final Level lv;

    public FormButtonHolder(Form f, @Nonnull Message author,@Nonnull Message msg, ConfigHolder config, boolean isFrame, boolean talent, boolean extra, boolean compact, Level lv, int lang, @Nonnull String channelID) {
        super(author, channelID, msg.getId());

        this.embed = msg;
        this.config = config;
        this.lang = lang;
        this.f = f;

        this.isFrame = isFrame;
        this.talent = talent;
        this.extra = extra;
        this.compact = compact;
        this.lv = lv;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), FormButtonHolder.this);

                expire(userID);
            }
        }, FIVE_MIN);
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent event) {
        event.getMessage().delete().queue();

        MessageChannel ch = event.getMessageChannel();

        if(event.getComponentId().equals("talent")) {
            if(f.du.getPCoin() == null)
                return;

            try {
                EntityHandler.showTalentEmbed(ch, getAuthorMessage(), f, isFrame, lang);
            } catch (Exception e) {
                StaticStore.logger.uploadErrorLog(e, "E/FormButtonHolder::handleEvent - Failed to show talent embed on button click");
            }
        } else {
            int diff = switch (event.getComponentId()) {
                case "first" -> -2;
                case "pre" -> -1;
                case "next" -> 1;
                case "final" -> 2;
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
                EntityHandler.showUnitEmb(newForm, ch, getAuthorMessage(), config, isFrame, talent, extra, false, false, lv, lang, false, compact);
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
