package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageInfoMessageHolder extends SearchHolder {
    private final List<Stage> stage;

    private final String additionalContent;

    private final TreasureHolder treasure;
    private final StageInfo.StageInfoConfig configData;

    public StageInfoMessageHolder(List<Stage> stage, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String keyword, ConfigHolder.SearchLayout layout, String additionalContent, TreasureHolder treasure, StageInfo.StageInfoConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, keyword, layout, lang);

        this.stage = stage;

        this.additionalContent = additionalContent;

        this.treasure = treasure;
        this.configData = configData;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1); i++) {
            if(i >= stage.size())
                break;

            Stage st = stage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            String fullName = "";

            if (mc != null) {
                String mcName = StaticStore.safeMultiLangGet(mc, lang);

                if (mcName == null || mcName.isBlank()) {
                    mcName = DataToString.getMapCode(mc);
                }

                fullName += mcName + " - ";
            } else {
                fullName += "Unknown - ";
            }

            String stmName = StaticStore.safeMultiLangGet(stm, lang);

            if (stmName == null || stmName.isBlank()) {
                stmName = Data.trio(stm.id.id);
            }

            fullName += stmName + " - ";

            String stName = StaticStore.safeMultiLangGet(st, lang);

            if (stName == null || stName.isBlank()) {
                stName = Data.trio(st.id.id);
            }

            fullName += stName;

            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        name = fullName;
                    } else {
                        name = "`" + DataToString.getStageCode(st) + "` " + fullName;
                    }
                }
                case LIST_DESCRIPTION -> name = DataToString.getStageCode(st);
                case LIST_LABEL -> name = fullName;
            }

            data.add(name);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        if(StaticStore.timeLimit.containsKey(userID)) {
            StaticStore.timeLimit.get(userID).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(userID, memberLimit);
        }

        try {
            EntityHandler.showStageEmb(stage.get(index), event, hasAuthorMessage() ? getAuthorMessage() : null, additionalContent, treasure, configData, true, true, lang, msg ->
                StaticStore.putHolder(userID, new StageInfoButtonHolder(stage.get(index), hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, msg, treasure, configData, true, lang))
            );
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/StageInfoMessageHolder::onSelected - Failed to upload stage embed");
        }
    }

    @Override
    public int getDataSize() {
        return stage.size();
    }
}
