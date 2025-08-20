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
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
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
    public void onSelected(GenericComponentInteractionCreateEvent event, int index) {
        MessageChannel ch = event.getChannel();
        Message author = getAuthorMessage();

        try {
            List<Stage> stages = EntityFilter.findStageByReward(rewards.get(index), chance, amount);

            if(stages.isEmpty()) {
                event.deferEdit()
                        .setComponents(TextDisplay.of(LangID.getStringByID("findReward.failed.noStage", lang).formatted(validateName(keyword))))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue();
            } else if(stages.size() == 1) {
                EntityHandler.generateStageEmbed(stages.getFirst(), event, getAuthorMessage(), "", treasure, configData, true, false, lang, result -> {
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
                event.deferEdit()
                        .setComponents(Command.getSearchComponents(stages.size(), LangID.getStringByID("findReward.several.reward", lang).formatted(validateName(keyword)), stages, this::accumulateStageTextData, layout, lang))
                        .useComponentsV2()
                        .setAllowedMentions(new ArrayList<>())
                        .mentionRepliedUser(false)
                        .queue(hook -> hook.retrieveOriginal().queue(msg -> StaticStore.putHolder(author.getAuthor().getId(), new StageInfoMessageHolder(stages, author, userID, ch.getId(), msg, keyword, layout, "", treasure, configData, lang))));
            }
        } catch (Exception e) {
            StaticStore.logger.uploadErrorLog(e, "E/FindRewardMessageHolder::onSelected - Failed to perform interaction");
        }
    }

    @Override
    public int getDataSize() {
        return rewards.size();
    }

    private List<String> accumulateStageTextData(List<Stage> stages, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(stages.size(), layout.chunkSize); i++) {
            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            if (stm.id == null || st.id == null)
                continue;

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

    private String validateName(String name) {
        if(name.length() > 1500)
            return name.substring(0, 1500) + "...";
        else
            return name;
    }
}
