package mandarin.packpack.commands.data;

import common.CommonStatic;
import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import mandarin.packpack.commands.ConstraintCommand;
import mandarin.packpack.supporter.StaticStore;
import mandarin.packpack.supporter.bc.CustomStageMap;
import mandarin.packpack.supporter.bc.EntityHandler;
import mandarin.packpack.supporter.lang.LangID;
import mandarin.packpack.supporter.server.data.IDHolder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StageStatAnalyzer extends ConstraintCommand {
    private static final List<String> allParameters = List.of(
            "-s", "-second", "-c", "-code", "-m", "-mid", "-s", "-l", "-len", "-n", "-name", "-lv",
            "-en", "-jp", "-tw", "-kr"
    );

    public StageStatAnalyzer(ROLE role, int lang, IDHolder id) {
        super(role, lang, id);
    }

    @Override
    public void prepare() throws Exception {
        registerRequiredPermission(Permission.MESSAGE_ATTACH_FILES);
    }

    @Override
    public void doSomething(GenericMessageEvent event) throws Exception {
        File temp = new File("./temp");

        if(!temp.exists() && !temp.mkdirs()) {
            StaticStore.logger.uploadLog("Can't create folder : "+temp.getAbsolutePath());
            return;
        }

        File container = StaticStore.generateTempFile(temp, "stat", "", true);

        if(container == null) {
            return;
        }

        MessageChannel ch = getChannel(event);
        Message author = getMessage(event);

        if(ch == null || author == null)
            return;

        String command = getContent(event);

        int mid = getMID(command);
        String code = getCode(command);

        if(mid == -1) {
            ch.sendMessage(LangID.getStringByID("stanalyzer_sid", lang)).queue();

            return;
        }

        if(code == null) {
            ch.sendMessage(LangID.getStringByID("stanalyzer_code", lang)).queue();

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

        if(!validateFile(workspace, code, mid, localeCode)) {
            ch.sendMessage("Couldn't find sufficient data for unit code : "+code+ " - "+mid).queue();

            return;
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
        List<Integer> indexes = new ArrayList<>();

        if(code.equals("CA") || code.equals("RA")) {
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
        } else if(name.length != len) {
            int nLen = name.length;

            ch.sendMessage(LangID.getStringByID("stat_name", lang).replace("_RRR_", len+"").replace("_PPP_", nLen+"")).queue();

            return;
        }

        VFile[] stages = new VFile[len];

        for(int i = 0; i < len; i++) {
            stages[i] = VFile.getFile(new File(dataLocal, "stageR" + code + Data.trio(mid % 1000) + "_" + Data.duo(i) + ".csv"));
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

        return 30;
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
            else
                return len;
        }

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

    private boolean validateFile(File workspace, String code, int mID, String locale) throws Exception {
        File dataLocal = new File(workspace, "DataLocal");
        File imageLocal = new File(workspace, "ImageLocal");
        File resLocal = new File(workspace, "resLocal");

        if(!dataLocal.exists() || !imageLocal.exists() || !resLocal.exists())
            return false;

        String loc;

        switch (locale) {
            case "en":
                loc = "en";
                break;
            case "zh":
                loc = "tw";
                break;
            case "kr":
                loc = "ko";
                break;
            default:
                loc = "ja";
        }

        File mapData = new File(dataLocal, "MapStageData" + code + "_" + Data.trio(mID % 1000) + ".csv");

        if(!mapData.exists())
            return false;

        int len = getStageLength(mapData);

        boolean newEnemy = false;

        for(int i = 0; i < len; i++) {
            File stageData = new File(dataLocal, "stageR" + code + Data.trio(mID % 1000) + "_" + Data.duo(i) + ".csv");

            if(!stageData.exists())
                return false;

            BufferedReader stageReader = new BufferedReader(new FileReader(stageData, StandardCharsets.UTF_8));

            stageReader.readLine();
            stageReader.readLine();

            String line;

            while((line = stageReader.readLine()) != null) {
                if(line.isBlank() || line.startsWith("0,"))
                    break;

                int[] enemyData = CommonStatic.parseIntsN(line);

                if(enemyData[0] - 2 >= UserProfile.getBCData().enemies.size()) {
                    newEnemy = true;

                    File enemyIcon = new File(imageLocal, "enemy_icon_"+Data.trio(enemyData[0] - 2)+".png");

                    if(!enemyIcon.exists()) {
                        VFile vf = VFile.get("./org/enemy/"+Data.trio(enemyData[0] - 2)+"/enemy_icon_"+Data.trio(enemyData[0] - 2)+".png");

                        if(vf == null)
                            return false;
                    }
                }
            }

            stageReader.close();
        }

        if(newEnemy) {
            File enemyName = new File(resLocal, "Enemyname.tsv");

            if(!enemyName.exists())
                return false;
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
                        if(reward < 1000)
                            newRewards.add(reward);
                        else if(reward < 10000)
                            newUnits.add(reward);
                        else if(reward < 30000)
                            newTrueForms.add(reward);
                    }
                }
            }

            if(!isTime && lineData.length > 9) {
                for(int j = 0; j < (lineData.length - 7) / 3; j++) {
                    int reward = lineData[6 + j * 3 + 1];

                    if(MultiLangCont.getStatic().RWNAME.getCont(reward) == null) {
                        if(reward < 1000)
                            newRewards.add(reward);
                        else if(reward < 10000)
                            newUnits.add(reward);
                        else if(reward < 30000)
                            newTrueForms.add(reward);
                    }
                }
            } else if(lineData.length != 6) {
                int reward = lineData[6];

                if(MultiLangCont.getStatic().RWNAME.getCont(reward) == null) {
                    if(reward < 1000)
                        newRewards.add(reward);
                    else if(reward < 10000)
                        newUnits.add(reward);
                    else if(reward < 30000)
                        newTrueForms.add(reward);
                }
            }
        }

        mapReader.close();

        if(!newRewards.isEmpty()) {
            File rewardText = new File(resLocal, "GatyaitemName.csv");

            if(!rewardText.exists())
                return false;

            for(int reward : newRewards) {
                File rewardIcon = new File(imageLocal, "gatyaitemD_"+Data.duo(reward)+"_f.png");

                if(!rewardIcon.exists())
                    return false;
            }
        }

        if(!newUnits.isEmpty()) {
            File unitDrop = new File(dataLocal, "drop_chara.csv");

            if(!unitDrop.exists())
                return false;

            BufferedReader dropReader = new BufferedReader(new FileReader(unitDrop, StandardCharsets.UTF_8));

            String line;

            while((line = dropReader.readLine()) != null) {
                if(line.isBlank())
                    break;

                int[] dropData = CommonStatic.parseIntsN(line);

                if(dropData.length != 3)
                    continue;

                if(newUnits.contains(dropData[0])) {
                    newUnits.remove(dropData[0]);

                    File unitBuy = new File(dataLocal, "unitbuy.csv");

                    if(!unitBuy.exists())
                        return false;

                    if(dropData[2] >= UserProfile.getBCData().units.size()) {
                        int egg = getEggValue(dropData[2], unitBuy);

                        String iconName = "gatyachara_" + Data.trio(egg != -1 ? egg : dropData[2]) + "_" + (egg != -1 ? "m" : "f") + ".png";

                        File icon = new File(imageLocal, iconName);
                        File name = new File(resLocal, "Unit_Explanation"+dropData[2]+"_"+loc+".csv");

                        if(!icon.exists() || !name.exists())
                            return false;
                    }
                }
            }

            dropReader.close();

            if(!newUnits.isEmpty())
                return false;
        }

        if(!newTrueForms.isEmpty()) {
            File unitBuy = new File(dataLocal, "unitbuy.csv");

            if(!unitBuy.exists())
                return false;

            BufferedReader buyReader = new BufferedReader(new FileReader(unitBuy, StandardCharsets.UTF_8));

            int count = 0;
            String line;

            while((line = buyReader.readLine()) != null) {
                String[] data = line.split(",");

                int reward = StaticStore.safeParseInt(data[23]);

                if(newTrueForms.contains(reward)) {
                    newTrueForms.remove(reward);

                    int egg = getEggValue(count, unitBuy);

                    String iconName = "gatyachara_" + Data.trio(egg != -1 ? egg : count) + "_" + (egg != -1 ? "m" : "f") + ".png";

                    File icon = new File(imageLocal, iconName);
                    File name = new File(resLocal, "Unit_Explanation"+Data.trio(count)+"_"+loc+".csv");

                    if(!icon.exists() || !name.exists())
                        return false;
                }

                count++;
            }
        }

        File mapOption = new File(dataLocal, "Map_option.csv");
        File stageOption = new File(dataLocal, "Stage_option.csv");
        File characterGroup = new File(dataLocal, "Charagroup.csv");

        return exists(mapOption, stageOption, characterGroup);
    }

    private boolean exists(File... files) {
        for(int i = 0; i < files.length; i++) {
            if(!files[i].exists())
                return false;
        }

        return true;
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
}
