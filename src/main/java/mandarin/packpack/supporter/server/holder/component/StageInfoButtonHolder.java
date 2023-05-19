package mandarin.packpack.supporter.server.holder.component;

import common.pack.Identifier;
import common.pack.UserProfile;
import common.util.pack.Background;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import common.util.stage.Music;
import common.util.stage.Stage;
import mandarin.packpack.commands.bc.Castle;
import mandarin.packpack.supporter.StaticStore;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class StageInfoButtonHolder extends ComponentHolder {
    private final Message embed;
    private final Stage st;
    private final boolean compact;

    public StageInfoButtonHolder(Stage st, Message author, Message msg, String channelID, boolean compact) {
        super(author, channelID, msg.getId());

        this.st = st;
        embed = msg;
        this.compact = compact;

        Timer autoFinish = new Timer();

        autoFinish.schedule(new TimerTask() {
            @Override
            public void run() {
                if(expired)
                    return;

                expired = true;

                StaticStore.removeHolder(author.getAuthor().getId(), StageInfoButtonHolder.this);

                expire(userID);
            }
        }, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    public void onEvent(GenericComponentInteractionCreateEvent ev) {
        expire(userID);

        if(!(ev instanceof ButtonInteractionEvent event)) {
            return;
        }

        switch (event.getComponentId()) {
            case "music" -> {
                if (st.mus0 == null)
                    return;

                Music ms = Identifier.get(st.mus0);

                if (ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    mandarin.packpack.commands.bc.Music.performButton(event, ms);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                }
            }
            case "music2" -> {
                if (st.mus1 == null)
                    return;

                Music ms = Identifier.get(st.mus1);

                if (ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    mandarin.packpack.commands.bc.Music.performButton(event, ms);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                }
            }
            case "bg" -> {
                Background bg = Identifier.get(st.bg);

                if (bg == null)
                    bg = UserProfile.getBCData().bgs.get(0);

                try {
                    mandarin.packpack.commands.bc.Background.performButton(event, bg);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for bg");
                }
            }
            case "castle" -> {
                CastleImg cs = Identifier.get(st.castle);

                if (cs == null) {
                    ArrayList<CastleList> lists = new ArrayList<>(CastleList.defset());

                    cs = lists.get(0).get(0);
                }

                try {
                    Castle.performButton(event, cs);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for castle");

                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire(String id) {
        if(compact) {
            embed.editMessageComponents()
                    .mentionRepliedUser(false)
                    .queue();
        } else {
            ArrayList<Button> buttons = new ArrayList<>();

            for(Button b : embed.getButtons()) {
                buttons.add(b.asDisabled());
            }

            embed.editMessageComponents(ActionRow.of(buttons))
                    .mentionRepliedUser(false)
                    .queue();
        }

        expired = true;
    }
}
