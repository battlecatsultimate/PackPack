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
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.Holder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;

public class StageInfoButtonHolder extends ComponentHolder {
    private Stage st;

    private final TreasureHolder treasure;
    private final StageInfo.StageInfoConfig configData;

    private final boolean switchable;

    public StageInfoButtonHolder(Stage st, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, TreasureHolder treasure, StageInfo.StageInfoConfig configData, boolean switchable, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.st = st;

        this.treasure = treasure;
        this.configData = configData;

        this.switchable = switchable;

        registerAutoExpiration(FIVE_MIN);
    }

    @Override
    public void onEvent(@Nonnull GenericComponentInteractionCreateEvent event) {
        switch (event.getComponentId()) {
            case "music" -> {
                disableButtons();

                if (!(event instanceof ButtonInteractionEvent ev))
                    return;

                if (st.mus0 == null)
                    return;

                Music ms = Identifier.get(st.mus0);

                if (ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    mandarin.packpack.commands.bc.Music.performButton(ev, ms);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                }

                end(true);
            }
            case "music2" -> {
                disableButtons();

                if (!(event instanceof ButtonInteractionEvent ev))
                    return;

                if (st.mus1 == null)
                    return;

                Music ms = Identifier.get(st.mus1);

                if (ms == null) {
                    ms = UserProfile.getBCData().musics.get(0);
                }

                try {
                    mandarin.packpack.commands.bc.Music.performButton(ev, ms);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for music");
                }

                end(true);
            }
            case "bg" -> {
                disableButtons();

                if (!(event instanceof ButtonInteractionEvent ev))
                    return;

                Background bg = Identifier.get(st.bg);

                if (bg == null)
                    bg = UserProfile.getBCData().bgs.get(0);

                try {
                    mandarin.packpack.commands.bc.Background.performButton(ev, bg);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for bg");
                }

                end(true);
            }
            case "castle" -> {
                disableButtons();

                if (!(event instanceof ButtonInteractionEvent ev))
                    return;

                CastleImg cs = Identifier.get(st.castle);

                if (cs == null) {
                    ArrayList<CastleList> lists = new ArrayList<>(CastleList.defset());

                    cs = lists.getFirst().get(0);
                }

                try {
                    Castle.performButton(ev, cs);
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder | Failed to prepare interaction data for castle");
                }

                end(true);
            }
            case "lineup" -> connectTo(event, new StageLineupHolder(st, hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, message, configData, lang));
            case "stage" -> {
                if (!(event instanceof StringSelectInteractionEvent ev))
                    return;

                int index = StaticStore.safeParseInt(ev.getValues().getFirst());

                if (index < 0 || index >= st.getCont().list.size())
                    return;

                st = st.getCont().list.get(index);

                try {
                    EntityHandler.generateStageEmbed(st, event, hasAuthorMessage() ? getAuthorMessage() : null, "", treasure, configData, true, switchable, lang, msg -> {});
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder::onEvent - Failed to switch stage to other stage");
                }
            }
            case "next" -> {
                int index = st.id.id + 1;

                if (index < 0 || index >= st.getCont().list.size())
                    return;

                st = st.getCont().list.get(index);

                try {
                    EntityHandler.generateStageEmbed(st, event, hasAuthorMessage() ? getAuthorMessage() : null, "", treasure, configData, true, switchable, lang, msg -> {});
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder::onEvent - Failed to switch stage to other stage");
                }
            }
            case "prev" -> {
                int index = st.id.id - 1;

                if (index < 0 || index >= st.getCont().list.size())
                    return;

                st = st.getCont().list.get(index);

                try {
                    EntityHandler.generateStageEmbed(st, event, hasAuthorMessage() ? getAuthorMessage() : null, "", treasure, configData, true, switchable, lang, msg -> {});
                } catch (Exception e) {
                    StaticStore.logger.uploadErrorLog(e, "E/StageInfoButtonHolder::onEvent - Failed to switch stage to other stage");
                }
            }
        }
    }

    @Override
    public void clean() {

    }

    @Override
    public void onExpire() {
        disableButtons();
    }

    private void disableButtons() {
        message.editMessageComponents(expireButton(message, configData.isCompact))
                .useComponentsV2()
                .setAllowedMentions(new ArrayList<>())
                .mentionRepliedUser(false)
                .queue();
    }

    @Override
    public void onConnected(@NotNull IMessageEditCallback event, @NotNull Holder parent) throws Exception {
        EntityHandler.generateStageEmbed(st, event, hasAuthorMessage() ? getAuthorMessage() : null, "", treasure, configData, true, switchable, lang, msg -> {});
    }
}
