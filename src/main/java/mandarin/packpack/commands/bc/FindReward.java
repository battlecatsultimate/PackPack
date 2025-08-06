package mandarin.packpack.commands.bc;

import common.CommonStatic;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.DataToString;
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.component.search.FindRewardMessageHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.component.search.StageInfoMessageHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FindReward extends TimedConstraintCommand {
    private static final int PARAM_COMPACT = 2;

    private final ConfigHolder config;

    public FindReward(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder idHolder, long time, ConfigHolder config) {
        super(role, lang, idHolder, time, StaticStore.COMMAND_FINDREWARD_ID, false);

        if(config == null)
            this.config = idHolder == null ? StaticStore.defaultConfig : idHolder.config;
        else
            this.config = config;
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_EXT_EMOJI, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_EMBED_LINKS);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        StageInfo.StageInfoConfig configData = new StageInfo.StageInfoConfig();
        String rewardName = getRewardName(loader.getContent());

        int param = checkParameters(loader.getContent());

        configData.isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        configData.isFrame = holder == null || holder.config.useFrame;
        configData.showDropInfo = config.showDropInfo;
        configData.showExtraStage = config.showExtraStage;
        configData.showMaterialDrop = config.showMaterialDrop;
        configData.showMiscellaneous = config.showMiscellaneous;

        double chance = getChance(loader.getContent());
        int amount = getAmount(loader.getContent());

        if(rewardName.isBlank()) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("findReward.failed.noName", lang));

            return;
        }

        if(chance != -1 && (chance <= 0 || chance > 100)) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("findReward.failed.invalidChance", lang));

            return;
        }

        if(amount != -1 && amount <= 0) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("findReward.failed.invalidAmount", lang));

            return;
        }

        List<Integer> rewards = EntityFilter.findRewardByName(rewardName, lang);

        if(rewards.isEmpty()) {
            replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("findReward.failed.noReward", lang).formatted(validateName(rewardName)));

            disableTimer();
        } else if(rewards.size() == 1) {
            List<Stage> stages = EntityFilter.findStageByReward(rewards.getFirst(), chance, amount);

            if(stages.isEmpty()) {
                replyToMessageSafely(ch, loader.getMessage(), LangID.getStringByID("findReward.failed.noStage", lang).formatted(validateName(rewardName)));

                disableTimer();
            } else if(stages.size() == 1) {
                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getMessage().getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showStageEmb(stages.getFirst(), ch, loader.getMessage(), "", treasure, configData, false, false, lang, result -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new StageInfoButtonHolder(stages.getFirst(), msg, u.getId(), ch.getId(), result, treasure, configData, false, lang));
                });
            } else {
                replyToMessageSafely(ch, loader.getMessage(), msg -> {
                    User u = loader.getUser();

                    TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                    StaticStore.putHolder(u.getId(), new StageInfoMessageHolder(stages, loader.getMessage(), u.getId(), ch.getId(), msg,  rewardName, config.searchLayout, "", treasure, configData, lang));
                }, getSearchComponents(stages.size(), LangID.getStringByID("findReward.several.stage", lang).formatted(validateName(rewardName)), stages, this::accumulateStageTextData, config.searchLayout, lang));
            }
        } else {
            replyToMessageSafely(ch, loader.getMessage(), msg -> {
                User u = loader.getUser();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                StaticStore.putHolder(u.getId(), new FindRewardMessageHolder(loader.getMessage(), u.getId(), ch.getId(), msg, rewardName, config.searchLayout, rewards, chance, amount, configData, treasure, lang));
            }, getSearchComponents(rewards.size(), LangID.getStringByID("findReward.several.reward", lang).formatted(validateName(rewardName)), rewards, this::accumulateRewardTextData, config.searchLayout, lang));

            disableTimer();
        }
    }

    private String getRewardName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean compact = false;
        boolean chance = false;
        boolean amount = false;

        for(int i = 1; i < contents.length; i++) {
            if (!compact && (contents[i].equals("-c") || contents[i].equals("-compact"))) {
                compact = true;
            } else if(!chance && (contents[i].equals("-ch") || contents[i].equals("-chance")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                chance = true;
                i++;
            } else if(!amount && (contents[i].equals("-a") || contents[i].equals("-amount")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                amount = true;
                i++;
            } else {
                result.append(contents[i]);

                if(i < contents.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString().trim();
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            label:
            for(String str : pureMessage) {
                switch (str) {
                    case "-c", "-compact" -> {
                        if ((result & PARAM_COMPACT) == 0) {
                            result |= PARAM_COMPACT;
                        } else
                            break label;
                    }
                }
            }
        }

        return result;
    }

    private double getChance(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-ch") || contents[i].equals("-chance")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                double value = Double.parseDouble(contents[i + 1]);

                if(value < 0)
                    return -100;
                else
                    return value;
            }
        }

        return -1;
    }

    private int getAmount(String content) {
        String[] contents = content.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-a") || contents[i].equals("-amount")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                int value = StaticStore.safeParseInt(contents[i + 1]);

                if(value < 0)
                    return -100;
                else
                    return value;
            }
        }

        return -1;
    }

    private List<String> accumulateRewardTextData(List<Integer> rewards, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(rewards.size(), config.searchLayout.chunkSize); i++) {
            if (i >= rewards.size())
                break;

            String text = null;

            switch (textType) {
                case TEXT -> {
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
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

    private List<String> accumulateStageTextData(List<Stage> stages, SearchHolder.TextType textType) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < Math.min(stages.size(), config.searchLayout.chunkSize); i++) {
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
                    if (config.searchLayout == ConfigHolder.SearchLayout.COMPACTED) {
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
