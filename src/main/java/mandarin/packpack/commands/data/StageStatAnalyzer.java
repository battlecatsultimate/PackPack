package mandarin.packpack.commands.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import common.CommonStatic;
import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.stage.Limit;
import common.util.stage.Stage;
import common.util.stage.StageLimit;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomStageMap;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.CommandLoader;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StageStatAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-c", "-code", "-m", "-mid", "-s", "-l", "-len", "-n", "-name", "-lv",
            "-en", "-jp", "-tw", "-kr", "-r", "-range"
    );

    public StageStatAnalyzer(ROLE role, CommonStatic.Lang.Locale lang, IDHolder id) {
        super(role, lang, id, false);
    }

    @Override
    public void prepare() {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(@Nonnull CommandLoader loader) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        MessageChannel ch = loader.getChannel();

        String command = loader.getContent();

        int mid = getMID(command);
        String code = getCode(command);

        if(mid == -1) {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.failed.noMapID", lang)).queue();

            return;
        }

        if(code == null) {
            ch.sendMessage(LangID.getStringByID("statAnalyzer.failed.noMapCode", lang)).queue();

            return;
        }

        int level = getLevel(command);
        boolean isSecond = isSecond(command);

        String localeCode = getLocale(command);

        File workspace = new File("./data/bc/"+localeCode+"/workspace");

        if(!workspace.exists()) {
            ch.sendMessage("Couldn't find workspace folder, try to call `p!da [Locale]` first").queue();

            return;
        }

        String result = validateFile(workspace, code, mid, localeCode);

        if(result != null) {
            ch.sendMessage("Couldn't find sufficient data for stage code : "+code+ " - "+mid + "\nReason : " + result).queue();

            return;
        }

        String stageCode;

        switch (code) {
            case "E" -> {
                code = "RE";
                stageCode = "EX";
            }
            case "RE" -> stageCode = "EX";
            case "G" -> {
                code = "G";
                stageCode = "G";
            }
            default -> stageCode = "R" + code;
        }

        File dataLocal = new File(workspace, "DataLocal");
        File imageLocal = new File(workspace, "ImageLocal");
        File resLocal = new File(workspace, "resLocal");

        File mapOption = new File(dataLocal, "Map_option.csv");

        String[] option = new String[0];

        BufferedReader optionReader = new BufferedReader(new FileReader(mapOption, StandardCharsets.UTF_8));

        optionReader.readLine();

        String line;

        while((line = optionReader.readLine()) != null) {
            String[] optionData = line.split(",");

            if(mid == StaticStore.safeParseInt(optionData[0])) {
                optionReader.close();

                option = optionData;

                break;
            }
        }

        File mapData = new File(dataLocal, "MapStageData" + code + "_" + Data.trio(mid % 1000) + ".csv");
        File stageOption = new File(dataLocal, "Stage_option.csv");
        File characterGroup = new File(dataLocal, "Charagroup.csv");

        int len = getStageLength(mapData);
        List<Integer> indexes = getStageRange(loader.getContent());

        for(int i = 0; i < indexes.size(); i++) {
            if(indexes.get(i) >= len) {
                createMessageWithNoPings(ch, LangID.getStringByID("statAnalyzer.failed.mapOutOfRange", lang));

                return;
            }
        }

        if(indexes.isEmpty() && (code.equals("CA") || code.equals("RA") || code.equals("A"))) {
            int index = 1;

            while(index < len) {
                indexes.add(index - 1);

                index += 10;
            }

            indexes.add(len - 1);
        }

        String[] name = getName(command);

        if(name == null) {
            if(!indexes.isEmpty()) {
                name = new String[indexes.size()];

                for(int i = 0; i < indexes.size(); i++) {
                    name[i] = code + " - " + Data.trio(mid % 1000) + " - " + Data.trio(indexes.get(i));
                }
            } else {
                name = new String[len];

                for(int i = 0; i < len; i++) {
                    name[i] = code + " - " + Data.trio(mid % 1000) + " - " + Data.trio(i);
                }
            }
        } else if(name.length != (indexes.isEmpty() ? len : indexes.size())) {
            int nLen = name.length;

            ch.sendMessage(LangID.getStringByID("statAnalyzer.failed.notEnoughName", lang).replace("_RRR_", String.valueOf(indexes.isEmpty() ? len : indexes.size())).replace("_PPP_", String.valueOf(nLen))).queue();

            return;
        }

        VFile[] stages = new VFile[len];

        for(int i = 0; i < len; i++) {
            stages[i] = VFile.getFile(new File(dataLocal, "stage" + stageCode + Data.trio(mid % 1000) + "_" + Data.duo(i) + ".csv"));
        }

        CustomStageMap map = new CustomStageMap(option, VFile.getFile(mapData), VFile.getFile(stageOption), VFile.getFile(characterGroup), stages, mid);

        if(!indexes.isEmpty())
            map.customIndex = indexes;

        map.convertRewardToUnit(new File(dataLocal, "unitbuy.csv"), new File(dataLocal, "drop_chara.csv"));

        if(!map.newEnemies.isEmpty()) {
            File enemyName = new File(resLocal, "Enemyname.tsv");

            map.handleNewEnemies(enemyName, imageLocal);
        }

        if(!map.newRewards.isEmpty()) {
            File rewardName = new File(resLocal, "GatyaitemName.csv");

            map.handleNewRewards(rewardName, imageLocal);
        }

        if(!map.newUnits.isEmpty()) {
            File unitDrop = new File(dataLocal, "drop_chara.csv");
            File unitBuy = new File(dataLocal, "unitbuy.csv");

            map.handleNewUnits(unitDrop, unitBuy, resLocal, imageLocal);
        }

        if(!map.newGroups.isEmpty()) {
            map.handleNewGroups(resLocal);
        }

        if (!map.newTrueForms.isEmpty()) {
            File unitBuy = new File(dataLocal, "unitbuy.csv");

            map.handleNewTrueForms(unitBuy, resLocal, imageLocal);
        }

        File specialRule = new File(dataLocal, "SpecialRulesMap.json");
        File specialRuleOption = new File(dataLocal, "SpecialRulesMapOption.json");

        FileReader ruleReader = new FileReader(specialRule, StandardCharsets.UTF_8);
        FileReader ruleOptionReader = new FileReader(specialRuleOption, StandardCharsets.UTF_8);

        JsonElement ruleElement = JsonParser.parseReader(ruleReader);
        JsonElement ruleOptionElement = JsonParser.parseReader(ruleOptionReader);

        ruleReader.close();
        ruleOptionReader.close();

        if (ruleElement.isJsonObject() && ruleOptionElement.isJsonObject()) {
            JsonObject rule = ruleElement.getAsJsonObject();
            JsonObject ruleOption = ruleOptionElement.getAsJsonObject();

            Map<Integer, List<Integer>> bannedComboData = new HashMap<>();

            JsonObject ruleList = ruleOption.getAsJsonObject("RuleType");

            for (String key : ruleList.keySet()) {
                JsonElement comboData = ruleList.get(key);

                int ruleID = CommonStatic.parseIntN(key);

                JsonArray comboArray = comboData.getAsJsonObject().getAsJsonArray("InvalidNyancomboID");

                List<Integer> bannedCombo = new ArrayList<>();

                for (JsonElement element : comboArray) {
                    if (!element.isJsonPrimitive())
                        continue;

                    bannedCombo.add(element.getAsInt());
                }

                bannedComboData.put(ruleID, bannedCombo);
            }

            JsonObject mapIDs = rule.getAsJsonObject("MapID");

            for (String id : mapIDs.keySet()) {
                int mapID = CommonStatic.safeParseInt(id);

                if (mapID != mid)
                    continue;

                JsonObject ruleData = mapIDs.getAsJsonObject(id);

                JsonObject ruleTypes = ruleData.getAsJsonObject("RuleType");

                for (String key : ruleTypes.keySet()) {
                    int ruleID = CommonStatic.parseIntN(key);
                    JsonObject parameterData = ruleTypes.getAsJsonObject(key);

                    JsonArray parameter = parameterData.getAsJsonArray("Parameters");

                    switch (ruleID) {
                        case 0 -> {
                            if (!parameter.isEmpty()) {
                                if (parameter.size() != 1)
                                    System.out.printf("W/MapColc::read - Unknown parameter data found for rule type %d : %s%n", ruleID, parameter);

                                int maxMoney = parameter.get(0).getAsInt();

                                List<Integer> bannedCombo = bannedComboData.compute(ruleID, (k, v) -> {
                                    if (v == null) {
                                        System.out.printf("W/MapColc::read - Unknown banned cat combo data found for rule type %d%n", ruleID);

                                        return new ArrayList<>();
                                    } else {
                                        return v;
                                    }
                                });

                                for (Stage stage : map.list) {
                                    if (stage.lim == null)
                                        stage.lim = new Limit();

                                    if (stage.lim.stageLimit == null)
                                        stage.lim.stageLimit = new StageLimit();

                                    stage.lim.stageLimit.maxMoney = maxMoney;
                                    stage.lim.stageLimit.bannedCatCombo.addAll(bannedCombo);
                                }
                            }
                        }
                        case 1 -> {
                            if (!parameter.isEmpty()) {
                                if (parameter.size() != 1)
                                    System.out.printf("W/MapColc::read - Unknown parameter data found for rule type %d : %s%n", ruleID, parameter);

                                int globalCooldown = parameter.get(0).getAsInt();

                                List<Integer> bannedCombo = bannedComboData.compute(ruleID, (k, v) -> {
                                    if (v == null) {
                                        System.out.printf("W/MapColc::read - Unknown banned cat combo data found for rule type %d%n", ruleID);

                                        return new ArrayList<>();
                                    } else {
                                        return v;
                                    }
                                });

                                for (Stage stage : map.list) {
                                    if (stage.lim == null)
                                        stage.lim = new Limit();

                                    if (stage.lim.stageLimit == null)
                                        stage.lim.stageLimit = new StageLimit();

                                    stage.lim.stageLimit.globalCooldown = globalCooldown;
                                    stage.lim.stageLimit.bannedCatCombo.addAll(bannedCombo);
                                }
                            }
                        }
                        case 5 -> {
                            if (!parameter.isEmpty()) {
                                int[] multiplier = new int[parameter.size()];

                                for (int i = 0; i < parameter.size(); i++) {
                                    multiplier[i] = parameter.get(i).getAsInt();
                                }

                                // To make program warn only once
                                boolean warned = false;

                                for (Stage stage : map.list) {
                                    if (stage.lim == null)
                                        stage.lim = new Limit();

                                    if (stage.lim.stageLimit == null) {
                                        stage.lim.stageLimit = new StageLimit();
                                    }

                                    if (!warned && stage.lim.stageLimit.costMultiplier.length != multiplier.length) {
                                        System.out.printf(
                                                "W/MapColc::read - Desynced cost multiplier data -> Original = %d, Obtained = %d, Data = [ %s ]%n",
                                                stage.lim.stageLimit.costMultiplier.length,
                                                multiplier.length,
                                                Arrays.toString(multiplier)
                                        );

                                        warned = true;
                                    }

                                    System.arraycopy(multiplier, 0, stage.lim.stageLimit.costMultiplier, 0, Math.min(stage.lim.stageLimit.costMultiplier.length, multiplier.length));
                                }
                            }
                        }
                        case 6 -> {
                            if (!parameter.isEmpty()) {
                                int[] multiplier = new int[parameter.size()];

                                for (int i = 0; i < parameter.size(); i++) {
                                    multiplier[i] = parameter.get(i).getAsInt();
                                }

                                // To make program warn only once
                                boolean warned = false;

                                for (Stage stage : map.list) {
                                    if (stage.lim == null)
                                        stage.lim = new Limit();

                                    if (stage.lim.stageLimit == null) {
                                        stage.lim.stageLimit = new StageLimit();
                                    }

                                    if (!warned && stage.lim.stageLimit.cooldownMultiplier.length != multiplier.length) {
                                        System.out.printf(
                                                "W/MapColc::read - Desynced cooldown multiplier data -> Original = %d, Obtained = %d, Data = [ %s ]%n",
                                                stage.lim.stageLimit.cooldownMultiplier.length,
                                                multiplier.length,
                                                Arrays.toString(multiplier)
                                        );

                                        warned = true;
                                    }

                                    System.arraycopy(multiplier, 0, stage.lim.stageLimit.cooldownMultiplier, 0, Math.min(stage.lim.stageLimit.cooldownMultiplier.length, multiplier.length));
                                }
                            }
                        }
                    }
                }
            }
        }

        if(level >= map.stars.length)
            level = map.stars.length - 1;

        EntityHandler.generateStageStatImage(ch, map, level, !isSecond, lang, name, code);
    }

    private int getMID(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-m") || contents[i].equals("-mid")) && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return -1;
    }

    private String getCode(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].equals("-c") || contents[i].equals("-code")) && i < contents.length - 1) {
                return contents[i + 1].toUpperCase(Locale.ENGLISH);
            }
        }

        return null;
    }

    private int getLevel(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-lv") && i < contents.length - 1 && StaticStore.isNumeric(contents[i + 1])) {
                return StaticStore.safeParseInt(contents[i + 1]);
            }
        }

        return 0;
    }

    private boolean isSecond(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-s") || contents[i].equals("-second"))
                return true;
        }

        return false;
    }

    private String[] getName(String command) {
        String[] contents = command.split(" ");

        for(int i = 0; i < contents.length; i++) {
            if(contents[i].equals("-n") || contents[i].equals("-name") && i < contents.length - 1) {
                StringBuilder builder = new StringBuilder();

                for(int j = i + 1; j < contents.length; j++) {
                    if(abortAppending(contents[j], "-n", "-name"))
                        break;

                    builder.append(contents[j]).append(" ");
                }

                String[] n = builder.toString().split(",");

                for(int j = 0; j < n.length; j++) {
                    n[j] = n[j].strip();
                }

                return n;
            }
        }

        return null;
    }

    private boolean abortAppending(String content, String... exception) {
        for(int i = 0; i < exception.length; i++) {
            if(content.equals(exception[i]))
                return false;
        }

        return allParameters.contains(content);
    }

    private int getStageLength(File mapData) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(mapData, StandardCharsets.UTF_8));

        reader.readLine();
        reader.readLine();

        int len = 0;
        String line;

        while((line = reader.readLine()) != null) {
            if(!line.isBlank())
                len++;
            else {
                reader.close();

                return len;
            }
        }

        reader.close();

        return len;
    }

    private String getLocale(String content) {
        if(content.contains("-en"))
            return "en";
        else if(content.contains("-tw"))
            return "zh";
        else if(content.contains("-kr"))
            return "kr";
        else
            return "jp";
    }

    private String validateFile(File workspace, String code, int mID, String locale) throws Exception {
        String stageCode;

        switch (code) {
            case "E" -> {
                code = "RE";
                stageCode = "EX";
            }
            case "RE" -> stageCode = "EX";
            case "G" -> {
                code = "G";
                stageCode = "G";
            }
            default -> stageCode = "R" + code;
        }

        File dataLocal = new File(workspace, "DataLocal");
        File imageLocal = new File(workspace, "ImageLocal");
        File resLocal = new File(workspace, "resLocal");

        if (!dataLocal.exists()) {
            return "No DataLocal folder found";
        }

        if (!imageLocal.exists()) {
            return "No ImageLocal folder found";
        }

        if (!resLocal.exists()) {
            return "No resLocal folder found";
        }

        String loc = switch (locale) {
            case "en" -> "en";
            case "zh" -> "tw";
            case "kr" -> "ko";
            default -> "ja";
        };

        File mapData = new File(dataLocal, "MapStageData" + code + "_" + Data.trio(mID % 1000) + ".csv");

        if(!mapData.exists()) {
            return "Failed to find MapStageData%s_%s.csv file in DataLocal".formatted(code, Data.trio(mID % 1000));
        }

        int len = getStageLength(mapData);

        boolean newEnemy = false;

        for(int i = 0; i < len; i++) {
            File stageData = new File(dataLocal, "stage" + stageCode + Data.trio(mID % 1000) + "_" + Data.duo(i) + ".csv");

            if(!stageData.exists()) {
                return "Failed to find stage%s%s_%s.csv file in DataLocal".formatted(stageCode, Data.trio(mID % 1000), Data.duo(i));
            }

            BufferedReader stageReader = new BufferedReader(new FileReader(stageData, StandardCharsets.UTF_8));

            stageReader.readLine();
            stageReader.readLine();

            String line;

            while((line = stageReader.readLine()) != null) {
                if(line.isBlank() || line.startsWith("0,"))
                    break;

                int[] enemyData = CommonStatic.parseIntsN(line);

                if (enemyData.length == 0)
                    break;

                if(enemyData[0] - 2 >= UserProfile.getBCData().enemies.size()) {
                    newEnemy = true;

                    File enemyIcon = new File(imageLocal, "enemy_icon_"+Data.trio(enemyData[0] - 2)+".png");

                    if(!enemyIcon.exists()) {
                        VFile vf = VFile.get("./org/enemy/"+Data.trio(enemyData[0] - 2)+"/enemy_icon_"+Data.trio(enemyData[0] - 2)+".png");

                        if(vf == null) {
                            stageReader.close();

                            return "Failed to find enemy icon in assets : %s".formatted("./org/enemy/"+Data.trio(enemyData[0] - 2)+"/enemy_icon_"+Data.trio(enemyData[0] - 2)+".png");
                        }
                    }
                }
            }

            stageReader.close();
        }

        if(newEnemy) {
            File enemyName = new File(resLocal, "Enemyname.tsv");

            if(!enemyName.exists())
                return "Failed to find Enemyname.tsv in resLocal";
        }

        List<Integer> newRewards = new ArrayList<>();
        List<Integer> newUnits = new ArrayList<>();
        List<Integer> newTrueForms = new ArrayList<>();

        BufferedReader mapReader = new BufferedReader(new FileReader(mapData, StandardCharsets.UTF_8));

        mapReader.readLine();
        mapReader.readLine();

        for(int i = 0; i < len; i++) {
            String line = mapReader.readLine();

            if(line == null)
                break;

            int[] lineData = CommonStatic.parseIntsN(line);

            boolean isTime = lineData.length > 15;

            if(isTime) {
                for(int j = 8; j < 15; j++) {
                    if(lineData[j] != -2) {
                        isTime = false;

                        break;
                    }
                }
            }

            if(isTime) {
                for(int j = 0; j < (lineData.length - 17) / 3; j++) {
                    int reward = lineData[16 + j * 3 + 1];

                    if(MultiLangCont.getStatic().RWNAME.getCont(reward) == null) {
                        if(reward < 1000 && !newRewards.contains(reward))
                            newRewards.add(reward);
                        else if(reward < 10000 && reward >= 1000 && !newUnits.contains(reward))
                            newUnits.add(reward);
                        else if(reward < 30000 && reward >= 10000 && !newTrueForms.contains(reward))
                            newTrueForms.add(reward);
                    }
                }
            }

            if(!isTime && lineData.length > 9) {
                for(int j = 0; j < (lineData.length - 7) / 3; j++) {
                    int reward = lineData[6 + j * 3 + 1];

                    if(MultiLangCont.getStatic().RWNAME.getCont(reward) == null) {
                        if(reward < 1000 && !newRewards.contains(reward))
                            newRewards.add(reward);
                        else if(reward < 10000 && reward >= 1000 && !newUnits.contains(reward))
                            newUnits.add(reward);
                        else if(reward < 30000 && reward >= 10000 && !newTrueForms.contains(reward))
                            newTrueForms.add(reward);
                    }
                }
            } else if(lineData.length != 6) {
                int reward = lineData[6];

                if(MultiLangCont.getStatic().RWNAME.getCont(reward) == null) {
                    if(reward < 1000 && !newRewards.contains(reward))
                        newRewards.add(reward);
                    else if(reward < 10000 && reward >= 1000 && !newUnits.contains(reward))
                        newUnits.add(reward);
                    else if(reward < 30000 && reward >= 10000 && !newTrueForms.contains(reward))
                        newTrueForms.add(reward);
                }
            }
        }

        mapReader.close();

        if(!newRewards.isEmpty()) {
            File rewardText = new File(resLocal, "GatyaitemName.csv");

            if(!rewardText.exists())
                return "Failed to find GatyaitemName.csv file in resLocal";

            for(int reward : newRewards) {
                File rewardIcon = new File(imageLocal, "gatyaitemD_"+Data.duo(reward)+"_f.png");

                if(!rewardIcon.exists())
                    return "Failed to find gatyaitemD_%s_f.png file in ImageLocal".formatted(Data.duo(reward));
            }
        }

        if(!newUnits.isEmpty()) {
            File unitDrop = new File(dataLocal, "drop_chara.csv");

            if(!unitDrop.exists())
                return "Failed to find drop_chara.csv in DataLocal";

            BufferedReader dropReader = new BufferedReader(new FileReader(unitDrop, StandardCharsets.UTF_8));

            String line;

            while((line = dropReader.readLine()) != null) {
                if(line.isBlank())
                    break;

                int[] dropData = CommonStatic.parseIntsN(line);

                if(dropData.length != 3)
                    continue;

                if(newUnits.contains(dropData[0])) {
                    newUnits.remove(Integer.valueOf(dropData[0]));

                    File unitBuy = new File(dataLocal, "unitbuy.csv");

                    if(!unitBuy.exists()) {
                        dropReader.close();

                        return "Failed to find unitbuy.csv in DataLocal";
                    }

                    if(dropData[2] >= UserProfile.getBCData().units.size()) {
                        int egg = getEggValue(dropData[2], unitBuy);

                        String iconName = "gatyachara_" + Data.trio(egg != -1 ? egg : dropData[2]) + "_" + (egg != -1 ? "m" : "f") + ".png";

                        File icon = new File(imageLocal, iconName);
                        File name = new File(resLocal, "Unit_Explanation"+(dropData[2]+ 1)+"_"+loc+".csv");

                        if(!icon.exists() || !name.exists()) {
                            dropReader.close();

                            if (!icon.exists()) {
                                return "Failed to find %s in ImageLocal".formatted(iconName);
                            } else {
                                return "Failed to find Unit_Explanation%d_%s.csv in resLocal".formatted(dropData[2] + 1, loc);
                            }
                        }
                    }
                }
            }

            dropReader.close();

            if(!newUnits.isEmpty())
                return "Failed to get new unit data as reward drop";
        }

        if(!newTrueForms.isEmpty()) {
            File unitBuy = new File(dataLocal, "unitbuy.csv");

            if(!unitBuy.exists())
                return "Failed to get unitbuy.csv in DataLocal";

            BufferedReader buyReader = new BufferedReader(new FileReader(unitBuy, StandardCharsets.UTF_8));

            int count = 0;
            String line;

            while((line = buyReader.readLine()) != null) {
                String[] data = line.split(",");

                int reward = StaticStore.safeParseInt(data[23]);

                if(newTrueForms.contains(reward)) {
                    newTrueForms.remove(Integer.valueOf(reward));

                    int egg = getEggValue(count, unitBuy);

                    String iconName = "gatyachara_" + Data.trio(egg != -1 ? egg : count) + "_" + (egg != -1 ? "m" : "f") + ".png";

                    File icon = new File(imageLocal, iconName);
                    File name = new File(resLocal, "Unit_Explanation"+Data.trio(count)+"_"+loc+".csv");

                    if(!icon.exists() || !name.exists()) {
                        buyReader.close();

                        if (!icon.exists()) {
                            return "Failed to get %s in ImageLocal".formatted(iconName);
                        } else {
                            return "Failed to get Unit_Explanation%s_%s.csv in resLocal".formatted(Data.trio(count), loc);
                        }
                    }
                }

                count++;
            }

            buyReader.close();
        }

        File mapOption = new File(dataLocal, "Map_option.csv");
        File stageOption = new File(dataLocal, "Stage_option.csv");
        File characterGroup = new File(dataLocal, "Charagroup.csv");
        File specialRule = new File(dataLocal, "SpecialRulesMap.json");
        File specialRuleOption = new File(dataLocal, "SpecialRulesMapOption.json");

        return exists(mapOption, stageOption, characterGroup, specialRule, specialRuleOption);
    }

    private String exists(File... files) {
        for(int i = 0; i < files.length; i++) {
            File parent = files[i].getParentFile();

            if(!files[i].exists())
                if (parent == null) {
                    return "Failed to find %s file".formatted(files[i].getName());
                } else {
                    return "Failed to find %s in %s".formatted(files[i].getName(), parent.getName());
                }
        }

        return null;
    }

    private int getEggValue(int uid, File unitBuy) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(unitBuy, StandardCharsets.UTF_8));

        int count = 0;
        String line;

        while((line = reader.readLine()) != null) {
            if(count == uid && !line.isBlank()) {
                reader.close();

                String[] data = line.split(",");

                int firstEgg = StaticStore.safeParseInt(data[data.length - 2]);
                int secondEgg = StaticStore.safeParseInt(data[data.length - 1]);

                if(secondEgg != -1)
                    return secondEgg;

                if(firstEgg != -1)
                    return firstEgg;

                break;
            }

            count++;
        }

        return -1;
    }

    private List<Integer> getStageRange(String content) {
        List<Integer> result = new ArrayList<>();

        String[] contents = content.replaceAll("(?<=\\d)( +)?,( +)?(?=\\d)", ",").replaceAll("(?<=\\d)( +)?-( +)?(?=\\d)", "-").split(" ");

        for(int i = 0; i < contents.length; i++) {
            if((contents[i].endsWith("-r") || contents[i].endsWith("-range")) && i < contents.length - 1) {
                String range = contents[i + 1];

                String[] segments = range.split(",");

                for(int j = 0; j < segments.length; j++) {
                    if(StaticStore.isNumeric(segments[j])) {
                        int n = StaticStore.safeParseInt(segments[j]);

                        if(!result.contains(n))
                            result.add(n);
                    } else {
                        String[] trial = segments[j].split("-");

                        if(trial.length == 2 && StaticStore.isNumeric(trial[0]) && StaticStore.isNumeric(trial[1])) {
                            int t0 = StaticStore.safeParseInt(trial[0]);
                            int t1 = StaticStore.safeParseInt(trial[1]);

                            for(int k = Math.min(t0, t1); k <= Math.max(t0, t1); k++) {
                                if(!result.contains(k))
                                    result.add(k);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }
}
