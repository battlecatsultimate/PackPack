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
import mandarin.packpack.supporter.bc.EntityFilter;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.ConfigHolder;
import mandarin.packpack.supporter.server.data.IDHolder;
import mandarin.packpack.supporter.server.data.TreasureHolder;
import mandarin.packpack.supporter.server.holder.component.StageInfoButtonHolder;
import mandarin.packpack.supporter.server.holder.component.search.SearchHolder;
import mandarin.packpack.supporter.server.holder.component.search.StageInfoMessageHolder;
import mandarin.packpack.supporter.server.slash.SlashOptionMap;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class StageInfo extends TimedConstraintCommand {
    public static class StageInfoConfig {
        public int star;

        public boolean isFrame;
        public boolean isCompact;
        public boolean showExtraStage;
        public boolean showDropInfo;
        public boolean showMaterialDrop;
        public boolean showMiscellaneous;
    }

    private static final int PARAM_SECOND = 2;
    private static final int PARAM_EXTRA = 4;
    private static final int PARAM_COMPACT = 8;
    private static final int PARAM_FRAME = 16;

    private static final int STAGE = 0;
    private static final int MAP = 1;
    private static final int COLLECTION = 2;

    private final ConfigHolder config;

    public StageInfo(ConstraintCommand.ROLE role, CommonStatic.Lang.Locale lang, IDHolder id, ConfigHolder config, long time) {
        super(role, lang, id, time, StaticStore.COMMAND_STAGEINFO_ID, false);

        if(config == null)
            this.config = id == null ? StaticStore.defaultConfig : id.config;
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

        String[] names;
        StageInfoConfig configData = new StageInfoConfig();

        if (loader.fromMessage) {
            String[] segments = loader.getContent().split(" ");

            StringBuilder removeMistake = new StringBuilder();

            for(int i = 0; i < segments.length; i++) {
                if(segments[i].matches("-lv(l)?(\\d+(,)?)+")) {
                    removeMistake.append("-lv ").append(segments[i].replace("-lvl", "").replace("-lv", ""));
                } else {
                    removeMistake.append(segments[i]);
                }

                if(i < segments.length - 1)
                    removeMistake.append(" ");
            }

            String command = removeMistake.toString();

            names = filterStageNames(command);

            int param = checkParameters(command);

            configData.star = getLevel(command);

            if ((param & PARAM_SECOND) > 0)
                configData.isFrame = false;
            else if ((param & PARAM_FRAME) > 0)
                configData.isFrame = true;
            else
                configData.isFrame = config.useFrame;

            configData.isCompact = (param & PARAM_COMPACT) > 0 || ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact);
        } else {
            SlashOptionMap optionMap = loader.getOptions();

            configData.star = optionMap.getOption("level", 0);

            names = new String[] {
                    optionMap.getOption("map_collection", ""),
                    optionMap.getOption("stage_map", ""),
                    optionMap.getOption("name", "")
            };

            configData.isFrame = optionMap.getOption("frame", config.useFrame);
            configData.isCompact = optionMap.getOption("compact", ((holder != null && holder.forceCompact) ? holder.config.compact : config.compact));
        }

        configData.showDropInfo = config.showDropInfo;
        configData.showExtraStage = config.showExtraStage;
        configData.showMaterialDrop = config.showMaterialDrop;
        configData.showMiscellaneous = config.showMiscellaneous;

        if(allNull(names)) {
            if (loader.fromMessage) {
                replyToMessageSafely(ch, LangID.getStringByID("stageInfo.fail.noParameter", lang), loader.getMessage(), a -> a);
            } else {
                replyToMessageSafely(loader.getInteractionEvent(), LangID.getStringByID("stageInfo.fail.noParameter", lang), a -> a);
            }

            disableTimer();
        } else {
            ArrayList<Stage> stages = EntityFilter.findStageWithName(names, lang);

            String additionalContent;

            if(stages.isEmpty() && names[0] == null && names[1] == null) {
                stages = EntityFilter.findStageWithMapName(names[2]);

                if(!stages.isEmpty()) {
                    additionalContent = LangID.getStringByID("stageInfo.smartSearch", lang) + "\n\n";
                } else {
                    additionalContent = "";
                }
            } else {
                additionalContent = "";
            }

            if(stages.isEmpty()) {
                if (loader.fromMessage) {
                    replyToMessageSafely(ch, additionalContent + LangID.getStringByID("stageInfo.fail.noResult", lang).replace("_", generateSearchName(names)), loader.getMessage(), a -> a);
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), additionalContent + "\n" + LangID.getStringByID("stageInfo.fail.noResult", lang).replace("_", generateSearchName(names)), a -> a);
                }

                disableTimer();
            } else if(stages.size() == 1) {
                TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                ArrayList<Stage> finalStages = stages;

                Object sender;

                if (loader.fromMessage) {
                    sender = ch;
                } else {
                    sender = loader.getInteractionEvent();
                }

                EntityHandler.showStageEmb(stages.getFirst(), sender, loader.getNullableMessage(), additionalContent, treasure, configData, false, true, lang, result ->
                    StaticStore.putHolder(loader.getUser().getId(), new StageInfoButtonHolder(finalStages.getFirst(), loader.getNullableMessage(), loader.getUser().getId(), ch.getId(), result, treasure, configData, true, lang))
                );
            } else {
                StringBuilder sb = new StringBuilder(additionalContent)
                        .append((LangID.getStringByID("stageInfo.several", lang).replace("_", generateSearchName(names))))
                        .append("```md\n")
                        .append(LangID.getStringByID("ui.search.selectData", lang));

                List<String> data = accumulateData(stages, true);

                for(int i = 0; i < data.size(); i++) {
                    sb.append(i+1).append(". ").append(data.get(i)).append("\n");
                }

                if(stages.size() > SearchHolder.PAGE_CHUNK) {
                    int totalPage = stages.size() / SearchHolder.PAGE_CHUNK;

                    if(stages.size() % SearchHolder.PAGE_CHUNK != 0)
                        totalPage++;

                    sb.append(LangID.getStringByID("ui.search.page", lang).formatted(1, totalPage)).append("\n");
                }

                sb.append("```");

                ArrayList<Stage> finalStages = stages;

                if (loader.fromMessage) {
                    replyToMessageSafely(ch, sb.toString(), loader.getMessage(), a -> registerSearchComponents(a, finalStages.size(), accumulateData(finalStages, false), lang), res -> {
                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                        StaticStore.putHolder(loader.getUser().getId(), new StageInfoMessageHolder(finalStages, loader.getNullableMessage(), loader.getUser().getId(), ch.getId(), res, additionalContent, treasure, configData, lang));
                    });
                } else {
                    replyToMessageSafely(loader.getInteractionEvent(), sb.toString(), a -> registerSearchComponents(a, finalStages.size(), accumulateData(finalStages, false), lang), res -> {
                        TreasureHolder treasure = holder != null && holder.forceFullTreasure ? TreasureHolder.global : StaticStore.treasure.getOrDefault(loader.getUser().getId(), TreasureHolder.global);

                        StaticStore.putHolder(loader.getUser().getId(), new StageInfoMessageHolder(finalStages, loader.getNullableMessage(), loader.getUser().getId(), ch.getId(), res, additionalContent, treasure, configData, lang));
                    });
                }

                disableTimer();
            }
        }
    }

    private boolean allNull(String[] name) {
        return name[0] == null && name[1] == null && name[2] == null;
    }

    private int checkParameters(String message) {
        String[] msg = message.split(" ");

        int result = 1;

        if(msg.length >= 2) {
            String[] pureMessage = message.split(" ", 2)[1].split(" ");

            for(String str : pureMessage) {
                if(str.startsWith("-s") && !str.startsWith("-stm")) {
                    if((result & PARAM_SECOND) == 0) {
                        result |= PARAM_SECOND;
                    } else
                        break;
                } else if(str.equals("-e") || str.equals("-extra")) {
                    if((result & PARAM_EXTRA) == 0) {
                        result |= PARAM_EXTRA;
                    } else
                        break;
                } else if(str.equals("-c") || str.equals("-compact")) {
                    if((result & PARAM_COMPACT) == 0) {
                        result |= PARAM_COMPACT;
                    } else
                        break;
                } else if (str.equals("-f") || str.equals("-fr")) {
                    if ((result & PARAM_FRAME) == 0) {
                        result |= PARAM_FRAME;
                    } else {
                        break;
                    }
                }
            }
        }

        return result;
    }

    private int getLevel(String command) {
        int level = 0;

        if(command.contains("-lv")) {
            String[] contents = command.split(" ");

            for(int i = 0; i < contents.length; i++) {
                if((contents[i].equals("-lv") || contents[i].equals("-lvl")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i+1])) {
                    level = StaticStore.safeParseInt(contents[i+1]);
                    break;
                }
            }
        }

        return level;
    }

    private String[] filterStageNames(String command) {
        String[] contents = command.split(" ");

        String[] names = new String[3];

        StringBuilder stage = new StringBuilder();
        StringBuilder map = new StringBuilder();
        StringBuilder collection = new StringBuilder();

        int mode = 0;

        boolean second = false;
        boolean frame = false;
        boolean level = false;
        boolean stm = false;
        boolean mc = false;
        boolean compact = false;
        boolean itf = false;
        boolean cotc = false;

        for(int i = 1; i < contents.length; i++) {
            if(!second && contents[i].equals("-s")) {
                second = true;
            } else if (!frame && contents[i].matches("-f(r)?")) {
                frame = true;
            } else if(!level && contents[i].matches("-lv(l)?") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                level = true;
                i++;
            } else if(!stm && contents[i].equals("-stm")) {
                stm = true;
                mode = MAP;
            } else if(!mc && contents[i].equals("-mc")) {
                mc = true;
                mode = COLLECTION;
            } else if(!compact && contents[i].matches("-c(ompact)?")) {
                compact = true;
            } else if(!itf && contents[i].matches("-i(tf)?\\d")) {
                itf = true;
            } else if(!cotc && contents[i].matches("-c(otc)?\\d")) {
                cotc = true;
            } else {
                switch (mode) {
                    case STAGE -> {
                        stage.append(contents[i]);

                        if (i < contents.length - 1) {
                            stage.append(" ");
                        }
                    }
                    case MAP -> {
                        map.append(contents[i]);

                        if (i < contents.length - 1) {
                            map.append(" ");
                        }
                    }
                    case COLLECTION -> {
                        collection.append(contents[i]);

                        if (i < contents.length - 1) {
                            collection.append(" ");
                        }
                    }
                }
            }
        }

        String result = stage.toString().trim();

        if(!result.isBlank()) {
            names[2] = result;
        }

        result = map.toString().trim();

        if(!result.isBlank()) {
            names[1] = result;
        }

        result = collection.toString().trim();

        if(!result.isBlank()) {
            names[0] = result;
        }

        return names;
    }

    private String generateSearchName(String[] names) {
        String result = "";

        for(int i = 0; i < names.length; i++) {
            if(names[i] != null && names[i].length() > 500) {
                names[i] = names[i].substring(0, 500) + "...";
            }
        }

        if(names[0] != null) {
            result += LangID.getStringByID("stageInfo.mapCollection", lang).replace("_", names[0])+", ";
        }

        if(names[1] != null) {
            result += LangID.getStringByID("stageInfo.stageMap", lang).replace("_", names[1])+", ";
        }

        if(names[2] != null) {
            result += LangID.getStringByID("stageInfo.stage", lang).replace("_", names[2]);
        }

        if(result.endsWith(", "))
            result = result.substring(0, result.length() -2);

        return result;
    }

    private List<String> accumulateData(List<Stage> stages, boolean full) {
        List<String> data = new ArrayList<>();

        for(int i = 0; i < SearchHolder.PAGE_CHUNK; i++) {
            if(i >= stages.size())
                break;

            Stage st = stages.get(i);
            StageMap stm = st.getCont();
            MapColc mc = stm.getCont();

            String name = "";

            if(full) {
                if(mc != null) {
                    String mcn = MultiLangCont.get(mc, lang);

                    if(mcn == null || mcn.isBlank())
                        mcn = mc.getSID();

                    name += mcn+" - ";
                } else {
                    name += "Unknown - ";
                }
            } else {
                name = "";
            }

            String stageMapName = MultiLangCont.get(stm, lang);

            if(stm.id != null) {
                if(stageMapName == null || stageMapName.isBlank())
                    stageMapName = Data.trio(stm.id.id);
            } else {
                if(stageMapName == null || stageMapName.isBlank())
                    stageMapName = "Unknown";
            }

            name += stageMapName+" - ";

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
}
