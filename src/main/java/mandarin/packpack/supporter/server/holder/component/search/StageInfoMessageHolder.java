package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
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

    public StageInfoMessageHolder(List<Stage> stage, @Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message message, String additionalContent, TreasureHolder treasure, StageInfo.StageInfoConfig configData, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, message, lang);

        this.stage = stage;

        this.additionalContent = additionalContent;

        this.treasure = treasure;
        this.configData = configData;
    }

    @Override
    public List<String> accumulateListData(boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = PAGE_CHUNK * page; i < PAGE_CHUNK * (page + 1); i++) {
            if(i >= stage.size())
                break;

            Stage st = stage.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(onText) {
                if(mc != null) {
                    String mcn = MultiLangCont.get(mc, lang);

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            }

            String stmn = MultiLangCont.get(stm, lang);

            if(stm.id != null) {
                if(stmn == null || stmn.isBlank())
                    stmn = Data.trio(stm.id.id);
            } else {
                if(stmn == null || stmn.isBlank())
                    stmn = "Unknown";
            }

            name += stmn+" - ";

            String stn = MultiLangCont.get(st, lang);

            if(st.id != null) {
                if(stn == null || stn.isBlank())
                    stn = Data.trio(st.id.id);
            } else {
                if(stn == null || stn.isBlank())
                    stn = "Unknown";
            }

            name += stn;

            data.add(name);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        int id = parseDataToInt(event);

        if(StaticStore.timeLimit.containsKey(userID)) {
            StaticStore.timeLimit.get(userID).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(userID, memberLimit);
        }

        try {
            EntityHandler.showStageEmb(stage.get(id), event, hasAuthorMessage() ? getAuthorMessage() : null, additionalContent, treasure, configData, true, true, lang, msg ->
                StaticStore.putHolder(userID, new StageInfoButtonHolder(stage.get(id), hasAuthorMessage() ? getAuthorMessage() : null, userID, channelID, msg, treasure, configData, true, lang))
            );
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/StageInfoMessageHolder::onSelected - Failed to upload stage embed");
        }
    }

    @Override
    public int getDataSize() {
        return stage.size();
    }

    @Override
    protected String getPage() {
        return additionalContent + super.getPage();
    }
}
