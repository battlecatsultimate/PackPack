package mandarin.packpack.supporter.server.holder.component;

import common.CommonStatic;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.util.pack.Background;
import common.util.stage.CastleImg;
import common.util.stage.CastleList;
import common.util.stage.Music;
import common.util.stage.Stage;
import mandarin.packpack.commands.bc.Castle;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;

public class StageInfoButtonHolder extends ComponentHolder {
    private final Stage st;
    private final boolean compact;

    public StageInfoButtonHolder(Stage st, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, boolean compact, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.st = st;
        this.compact = compact;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent ev) {
        disableButtons();

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

                    cs = lists.getFirst().get(0);
                }

                try {
                    Castle.performButton(event, cs);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for castle");
                }
            }
            case "lineup" -> {
                try {
                    EntityHandler.showFixedLineupData(st, st.preset, event, lang);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder::onEvent - Failed to show fixed lineup data embed");
                }
            }
        }

        end(true);
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        disableButtons();
    }

    private void disableButtons() {
        if(compact) {
            message.editMessageComponents()
                    .mentionRepliedUser(false)
                    .queue();
        } else {
            ArrayList<Button> buttons = new ArrayList<>();

            for(Button b : message.getButtons()) {
                buttons.add(b.asDisabled());
            }

            message.editMessageComponents(ActionRow.of(buttons))
                    .mentionRepliedUser(false)
                    .queue();
        }
    }
}
