package mandarin.packpack.commands.bc;

import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.MapColc;
import common.util.stage.Stage;
import common.util.stage.StageMap;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.commands.TimedConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FindReward extends TimedConstraintCommand {
    private static final int PARAM_EXTRA = 2;
    private static final int PARAM_COMPACT = 4;

    private final ConfigHolder config;

    public FindReward(ConstraintCommand.ROLE role, int lang, IDHolder idHolder, long time, ConfigHolder config) {
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
    public void doSomething(@NotNull CommandLoader loader) throws Exception {
        MessageChannel ch = loader.getChannel();

        String rewardName = getRewardName(loader.getContent());

        int param = checkParameters(loader.getContent());

        boolean isExtra = (param & PARAM_EXTRA) > 0 || config.extra;
        boolean isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);

        double chance = getChance(loader.getContent());
        int amount = getAmount(loader.getContent());

        if(rewardName.isBlank()) {
            replyToMessageSafely(ch, LangID.getStringByID("freward_noname", lang), loader.getMessage(), a -> a);

            return;
        }

        if(chance != -1 && (chance <= 0 || chance > 100)) {
            replyToMessageSafely(ch, LangID.getStringByID("freward_chance", lang), loader.getMessage(), a -> a);

            return;
        }

        if(amount != -1 && amount <= 0) {
            replyToMessageSafely(ch, LangID.getStringByID("freward_amount", lang), loader.getMessage(), a -> a);

            return;
        }

        List<Integer> rewards = EntityFilter.findRewardByName(rewardName, lang);

        if(rewards.isEmpty()) {
            replyToMessageSafely(ch, LangID.getStringByID("freward_norew", lang).replace("_", validateName(rewardName)), loader.getMessage(), a -> a);

            disableTimer();
        } else if(rewards.size() == 1) {
            List<Stage> stages = EntityFilter.findStageByReward(rewards.getFirst(), chance, amount);

            if(stages.isEmpty()) {
                replyToMessageSafely(ch, LangID.getStringByID("freward_nosta", lang).replace("_", validateName(rewardName)), loader.getMessage(), a -> a);

                disableTimer();
            } else if(stages.size() == 1) {
                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getMessage().getAuthor().getId(), TreasureHolder.global);

                EntityHandler.showStageEmb(stages.getFirst(), ch, loader.getMessage(), holder == null || holder.config.useFrame, isExtra, isCompact, 0, treasure, lang, result -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    StaticStore.putHolder(u.getId(), new StageInfoButtonHolder(stages.getFirst(), msg, result, ch.getId(), isCompact));
                });
            } else {
                StringBuilder sb = new StringBuilder(LangID.getStringByID("freward_severalst", lang).replace("_", validateName(rewardName)))
                        .append("```md\n")
                        .append(LangID.getStringByID("formst_pick", lang));

                List<String> data = accumulateStage(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = stages.size() / SearchHolder.PAGE_CHUNK;

                    if(stages.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
                }

                sb.append("```");

                replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, stages.size(), accumulateStage(stages, false), lang), res -> {
                    User u = loader.getUser();

                    Message msg = loader.getMessage();

                    TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                    StaticStore.putHolder(u.getId(), new StageInfoMessageHolder(stages, msg, res, ch.getId(), 0, treasure, config.useFrame, isExtra, isCompact, lang));
                });


            }
        } else {
            StringBuilder sb = new StringBuilder(LangID.getStringByID("freward_several", lang).replace("_", validateName(rewardName)))
                    .append("```md\n")
                    .append(LangID.getStringByID("formst_pick", lang));

            List<String> data = accumulateReward(rewards);

            for(int i = 0; i < data.size(); i++) {
                sb.append(i+1).append(". ").append(data.get(i)).append("\n");
            }

            if(rewards.size() > SearchHolder.PAGE_CHUNK) {
                int totalPage = rewards.size() / SearchHolder.PAGE_CHUNK;

                if(rewards.size() % SearchHolder.PAGE_CHUNK != 0)
                    totalPage++;

                sb.append(LangID.getStringByID("formst_page", lang).replace("_", String.valueOf(1)).replace("-", String.valueOf(totalPage))).append("\n");
            }

            sb.append("```");

            registerSearchComponents(ch.sendMessage(sb.toString()).setAllowedMentions(new ArrayList<>()), rewards.size(), data, lang).queue(res -> {
                User u = loader.getUser();

                Message msg = loader.getMessage();

                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(u.getId(), TreasureHolder.global);

                StaticStore.putHolder(u.getId(), new FindRewardMessageHolder(res, msg, ch.getId(), rewards, rewardName, chance, amount, isExtra, isCompact, config.useFrame, treasure, lang));
            });

            disableTimer();
        }
    }

    private String getRewardName(String message) {
        String[] contents = message.split(" ");

        if(contents.length < 2)
            return "";

        StringBuilder result = new StringBuilder();

        boolean extra = false;
        boolean compact = false;
        boolean chance = false;
        boolean amount = false;

        for(int i = 1; i < contents.length; i++) {
            if (!extra && (contents[i].equals("-e") || contents[i].equals("-extra"))) {
                extra = true;
            } else if (!compact && (contents[i].equals("-c") || contents[i].equals("-compact"))) {
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
                    case "-e", "-extra" -> {
                        if ((result & PARAM_EXTRA) == 0) {
                            result |= PARAM_EXTRA;
                        } else
                            break label;
                    }
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

    private List<String> accumulateReward(List<Integer> rewards) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= rewards.size())
                break;

            String rname = Data.trio(rewards.get(i)) + " ";

            String name = MultiLangCont.getStatic().RWNAME.getCont(rewards.get(i), lang);

            if(name != null && !name.isBlank()) {
                rname += name;
            }

            data.add(rname);
        }

        return data;
    }

    private List<String> accumulateStage(List<Stage> stage, boolean onText) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
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
