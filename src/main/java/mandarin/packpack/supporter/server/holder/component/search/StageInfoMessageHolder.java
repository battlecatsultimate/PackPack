package mandarin.packpack.supporter.server.holder.component.search;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StageInfoMessageHolder extends SearchHolder {
    private final List<Stage> stage;
    private final Message author;

    private final boolean isFrame;
    private final boolean isExtra;
    private final boolean isCompact;
    private final int star;
    private final TreasureHolder treasure;

    public StageInfoMessageHolder(List<Stage> stage, Message author, Message msg, String channelID, int star, TreasureHolder treasure, boolean isFrame, boolean isExtra, boolean isCompact, int lang) {
        super(author, msg, channelID, lang);

        this.stage = stage;
        this.author = author;

        this.star = star;
        this.treasure = treasure;
        this.isFrame = isFrame;
        this.isExtra = isExtra;
        this.isCompact = isCompact;

        registerAutoFinish(this, msg, lang, FIVE_MIN);
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
        MessageChannel ch = event.getChannel();

        int id = parseDataToInt(event);

        msg.delete().queue();

        String mid = author.getAuthor().getId();

        if(StaticStore.timeLimit.containsKey(mid)) {
            StaticStore.timeLimit.get(mid).put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());
        } else {
            Map<String, Long> memberLimit = new HashMap<>();

            memberLimit.put(StaticStore.COMMAND_STAGEINFO_ID, System.currentTimeMillis());

            StaticStore.timeLimit.put(mid, memberLimit);
        }

        try {
            Message msg = EntityHandler.showStageEmb(stage.get(id), ch, getAuthorMessage(), isFrame, isExtra, isCompact, star, treasure, lang);

            if(msg != null) {
                StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stage.get(id), author, msg, channelID, isCompact));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getDataSize() {
        return stage.size();
    }
}
