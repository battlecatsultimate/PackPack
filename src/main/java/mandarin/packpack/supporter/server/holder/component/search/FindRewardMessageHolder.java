package mandarin.packpack.supporter.server.holder.component.search;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.Command;
import mandarin.packpack.commands.bc.StageInfo;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindRewardMessageHolder extends SearchHolder {
    private final List<Integer> rewards;
    private final String keyword;

    private final double chance;
    private final int amount;

    private final TreasureHolder treasure;
    private final StageInfo.StageInfoConfig configData;

    public FindRewardMessageHolder(@Nullable Message author, @Nonnull String userID, @Nonnull String channelID, @Nonnull Message msg, String keyword, ConfigHolder.SearchLayout layout, List<Integer> rewards, double chance, int amount, StageInfo.StageInfoConfig configData, TreasureHolder treasure, CommonStatic.Lang.Locale lang) {
        super(author, userID, channelID, msg, keyword, layout, lang);

        this.rewards = rewards;
        this.keyword = keyword;

        this.chance = chance;
        this.amount = amount;

        this.treasure = treasure;
        this.configData = configData;
    }

    @Override
    public List<String> accumulateTextData(TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = chunk * page; i < chunk * (page + 1); i++) {
            if (i >= rewards.size())
                break;

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (layout == ConfigHolder.SearchLayout.COMPACTED) {
                        text = Data.trio(rewards.get(i));

                        String rewardName = MultiLangCont.getStatic().RWNAME.getCont(rewards.get(i), lang);

                        if (rewardName != null && !rewardName.isBlank()) {
                            text += " " + rewardName;
                        }
                    } else {
                        text = "`" + Data.trio(rewards.get(i)) + "`";

                        String rewardName = MultiLangCont.getStatic().RWNAME.getCont(rewards.get(i), lang);

                        if (rewardName == null || rewardName.isBlank())
                            rewardName = Data.trio(rewards.get(i));

                        text += " " + rewardName;
                    }
                }
                case LIST_LABEL -> {
                    text = MultiLangCont.getStatic().RWNAME.getCont(rewards.get(i), lang);

                    if (text == null || text.isBlank())
                        text = Data.trio(rewards.get(i));
                }
                case LIST_DESCRIPTION -> text = Data.trio(rewards.get(i));
            }

            data.add(text);
        }

        return data;
    }

    @Override
    public void onSelected(GenericComponentInteractionCreateEvent event) {
        MessageChannel ch = event.getChannel();
        Message author = getAuthorMessage();

        try {
            int id = parseDataToInt(event);

            message.delete().queue();

            List<Stage> stages = EntityFilter.findStageByReward(rewards.get(id), chance, amount);

            if(stages.isEmpty()) {
                ch.sendMessage(LangID.getStringByID("findReward.failed.noStage", lang).replace("_", validateName(keyword))).queue();
            } else if(stages.size() == 1) {
                EntityHandler.showStageEmb(stages.getFirst(), ch, getAuthorMessage(), "", treasure, configData, true, false, lang, result -> {
                    if(StaticStore.timeLimit.containsKey(author.getAuthor().getId())) {
                        StaticStore.timeLimit.get(author.getAuthor().getId()).put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());
                    } else {
                        Map<String, Long> memberLimit = new HashMap<>();

                        memberLimit.put(StaticStore.COMMAND_FINDSTAGE_ID, System.currentTimeMillis());

                        StaticStore.timeLimit.put(author.getAuthor().getId(), memberLimit);
                    }

                    StaticStore.putHolder(author.getAuthor().getId(), new StageInfoButtonHolder(stages.getFirst(), author, userID, channelID, result, treasure, configData, false, lang));
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("findReward.several.reward", lang).replace("_", validateName(keyword))).append("```md\n");

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > chunk) {
                    int totalPage = getTotalPage(stages.size(), chunk);

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                Command.registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), stages.size(), accumulateStage(stages, false), lang).queue(res -> {
                    if(res != null) {
                        StaticStore.putHolder(author.getAuthor().getId(), new StageInfoMessageHolder(stages, author, userID, ch.getId(), res, keyword, layout, "", treasure, configData, lang));
                    }
                });


            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FindRewardMessageHolder::onSelected - Failed to perform interaction");
        }
    }

    @Override
    public int getDataSize() {
        return rewards.size();
    }

    private List<String> accumulateStage(List<Stage> stage, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < ConfigHolder.SearchLayout.COMPACTED.chunkSize; i++) {
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

    private String validateName(String name) {
        if(name.length() > 1500)
            return name.substring(0, 1500) + "...";
        else
            return name;
    }
}
