package mandarin.packpack.supporter.bc;

import common.CommonStatic;
import common.pack.Identifier;
import common.pack.UserProfile;
import common.system.files.VFile;
import common.util.Data;
import common.util.lang.MultiLangCont;
import common.util.pack.Background;
import common.util.stage.*;
import common.util.stage.info.DefStageInfo;
import common.util.unit.Unit;
import mandarin.packpack.supporter.StaticStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CustomStageMap extends StageMap {
    public final int mapID;

    public final Map<Integer, String> rewardToUnitIcon = new HashMap<>();

    public final List<Integer> newEnemies = new ArrayList<>();
    public final List<Integer> newRewards = new ArrayList<>();
    public final List<Integer> newUnits = new ArrayList<>();
    public final List<Integer> newGroups = new ArrayList<>();
    public final List<Integer> newTrueForms = new ArrayList<>();

    public final Map<Integer, String> enemyNames = new HashMap<>();
    public final Map<Integer, String> rewardNames = new HashMap<>();
    public final Map<Integer, String> unitNames = new HashMap<>();

    public final Map<Integer, File> enemyIcons = new HashMap<>();
    public final Map<Integer, File> rewardIcons = new HashMap<>();
    public final Map<Integer, File> unitIcons = new HashMap<>();
    public final Map<Integer, File> trueFormIcons = new HashMap<>();

    public List<Integer> customIndex = new ArrayList<>();

    public CustomStageMap(String[] option, VFile data, VFile stageOption, VFile characterGroup, VFile[] stages, int mapID) {
        this.mapID = mapID;

        int len = StaticStore.safeParseInt(option[1]);

        stars = new int[len];

        for(int i = 0; i < len; i++) {
            stars[i] = StaticStore.safeParseInt(option[2 + i]);
        }

        starMask = StaticStore.safeParseInt(option[12]);

        info = new StageMapInfo(this);

        if(!option[7].equals("0")) {
            info.resetMode = StaticStore.safeParseInt(option[7]);
        }

        if(!option[8].equals("0")) {
            info.clearLimit = StaticStore.safeParseInt(option[8]);
        }

        info.hiddenUponClear = !option[13].equals("0");

        if(!option[10].equals("0")) {
            info.waitTime = StaticStore.safeParseInt(option[10]);
        }

        Queue<String> dataLines = data.getData().readLine();

        dataLines.poll();
        dataLines.poll();

        for(int i = 0; i < stages.length; i++) {
            Stage st = new CustomStage(this);

            st.lim = new Limit();

            String dataLine = dataLines.poll();

            if(dataLine == null)
                continue;

            int[] stageInt = CommonStatic.parseIntsN(dataLine.split("//")[0]);

            st.info = new DefStageInfo(info, st, stageInt);

            Queue<String> stageData = stages[i].getData().readLine();

            String initialLine = stageData.poll();

            if(initialLine == null)
                continue;

            String[] initialData = initialLine.split(",");

            st.castle = Identifier.parseInt(StaticStore.safeParseInt(initialData[0]), CastleImg.class);
            st.non_con = initialData[1].equals("1");

            ((DefStageInfo) st.info).setData(initialData);

            String secondaryLine = stageData.poll();

            if(secondaryLine == null)
                continue;

            String[] secondaryData = secondaryLine.split(",");

            st.len = StaticStore.safeParseInt(secondaryData[0]);
            st.health = StaticStore.safeParseInt(secondaryData[1]);

            st.minSpawn = StaticStore.safeParseInt(secondaryData[2]);
            st.maxSpawn = StaticStore.safeParseInt(secondaryData[3]);

            st.bg = Identifier.rawParseInt(StaticStore.safeParseInt(secondaryData[4]), Background.class);

            st.max = StaticStore.safeParseInt(secondaryData[5]);

            st.timeLimit = secondaryData.length >= 8 ? Math.max(StaticStore.safeParseInt(secondaryData[7]), 0) : 0;

            if(st.timeLimit != 0)
                st.health = Integer.MAX_VALUE;

            st.trail = st.timeLimit != 0;

            int isBase = StaticStore.safeParseInt(secondaryData[6]) - 2;

            List<int[]> lineList = new ArrayList<>();

            while(stageData.size() > 0) {
                String line = stageData.poll();

                if(line == null || line.isBlank())
                    break;

                if(!Character.isDigit(line.charAt(0)))
                    break;

                if(line.startsWith("0,"))
                    break;

                String[] lineRaw = line.split(",");

                int[] lineData = new int[SCDef.SIZE];

                for(int j = 0; j < 10; j++) {
                    if(j < lineRaw.length) {
                        lineData[j] = StaticStore.safeParseInt(lineRaw[j]);
                    } else if(j == 9) {
                        lineData[j] = 100;
                    }
                }

                lineData[0] -= 2;
                lineData[2] *= 2;
                lineData[3] *= 2;
                lineData[4] *= 2;

                if(st.timeLimit == 0 && lineData[5] > 100 && lineData[9] == 100) {
                    lineData[9] = lineData[5];
                    lineData[5] = 100;
                }

                if(lineRaw.length > 11 && StaticStore.isNumeric(lineRaw[11])) {
                    lineData[SCDef.M1] = StaticStore.safeParseInt(lineRaw[11]);

                    if(lineData[SCDef.M1] == 0)
                        lineData[SCDef.M1] = lineData[SCDef.M];
                } else {
                    lineData[SCDef.M1] = lineData[SCDef.M];
                }

                if(lineRaw.length > 12 && StaticStore.isNumeric(lineRaw[12]) && StaticStore.safeParseInt(lineRaw[12]) == 1) {
                    lineData[SCDef.S0] *= -1;
                }

                if(lineRaw.length > 13 && StaticStore.isNumeric(lineRaw[13])) {
                    lineData[SCDef.KC] = StaticStore.safeParseInt(lineRaw[13]);
                }

                if(lineData[0] == isBase)
                    lineData[SCDef.C0] = 0;

                if(lineData[0] >= UserProfile.getBCData().enemies.size() && !newEnemies.contains(lineData[0])) {
                    newEnemies.add(lineData[0]);
                }

                lineList.add(lineData);
            }

            SCDef def = new SCDef(lineList.size());

            for(int j = 0; j < lineList.size(); j++) {
                def.datas[j] = new SCDef.Line(lineList.get(def.datas.length - j - 1));
            }

            if(StaticStore.safeParseInt(secondaryData[6]) == 317) {
                def.datas[lineList.size() - 1].castle_0 = 0;
            }

            st.data = def;

            if(((DefStageInfo) st.info).drop != null && ((DefStageInfo) st.info).drop.length != 0) {
                for(int[] drop : ((DefStageInfo) st.info).drop) {
                    if(MultiLangCont.getStatic().RWNAME.getCont(drop[1]) == null) {
                        if(drop[1] < 1000) {
                            if (!newRewards.contains(drop[1])) {
                                newRewards.add(drop[1]);
                            }
                        } else if(drop[1] < 10000) {
                            if(!newUnits.contains(drop[1])) {
                                newUnits.add(drop[1]);
                            }
                        } else if(drop[1] < 30000) {
                            if(!newTrueForms.contains(drop[1])) {
                                newTrueForms.add(drop[1]);
                            }
                        }
                    }
                }
            }

            if(((DefStageInfo) st.info).time != null && ((DefStageInfo) st.info).time.length != 0) {
                for(int[] drop : ((DefStageInfo) st.info).time) {
                    if(MultiLangCont.getStatic().RWNAME.getCont(drop[1]) == null) {
                        if(drop[1] < 1000) {
                            if (!newRewards.contains(drop[1])) {
                                newRewards.add(drop[1]);
                            }
                        } else if(drop[1] < 10000) {
                            if(!newUnits.contains(drop[1])) {
                                newUnits.add(drop[1]);
                            }
                        } else if(drop[1] < 30000) {
                            if(!newTrueForms.contains(drop[1])) {
                                newTrueForms.add(drop[1]);
                            }
                        }
                    }
                }
            }

            list.add(st);
        }

        Map<Integer, CharaGroup> groups = new HashMap<>();

        Queue<String> groupLine = characterGroup.getData().readLine();

        groupLine.poll();

        while(groupLine.size() > 0) {
            String line = groupLine.poll();

            if(line == null)
                break;

            String[] groupData = line.split(",");

            int id = StaticStore.safeParseInt(groupData[0]);
            int type = StaticStore.safeParseInt(groupData[2]);

            List<Identifier<Unit>> units = new ArrayList<>();

            for(int i = 3; i < groupData.length; i++) {
                units.add(Identifier.parseInt(StaticStore.safeParseInt(groupData[i]), Unit.class));
            }

            groups.put(id, new CustomCharaGroup(units, type));
        }

        Queue<String> limitLine = stageOption.getData().readLine();

        limitLine.poll();

        while (limitLine.size() > 0) {
            String line = limitLine.poll();

            if(line == null || line.isBlank())
                break;

            String[] limitData = line.split(",");

            if(StaticStore.safeParseInt(limitData[0]) != mapID)
                continue;

            Limit limit = new Limit();

            limit.star = StaticStore.safeParseInt(limitData[1]);
            limit.sid = StaticStore.safeParseInt(limitData[2]);
            limit.rare = StaticStore.safeParseInt(limitData[3]);
            limit.num = StaticStore.safeParseInt(limitData[4]);
            limit.line = StaticStore.safeParseInt(limitData[5]);
            limit.min = StaticStore.safeParseInt(limitData[6]);
            limit.max = StaticStore.safeParseInt(limitData[7]);
            limit.group = groups.get(StaticStore.safeParseInt(limitData[8]));

            if(limit.sid == -1) {
                for(int i = 0; i < list.size(); i++) {
                    list.get(i).lim = limit;
                }
            } else {
                list.get(limit.sid).lim = limit;
            }
        }
    }

    @Override
    public MapColc getCont() {
        if(mapID / 1000 == 14) {
            return MapColc.get("000014");
        } else {
            return MapColc.get("000000");
        }
    }

    public void convertRewardToUnit(File unitBuy, File unitDrop) {
        List<Integer> requiredIDs = new ArrayList<>();

        VFile dropFile = VFile.getFile(unitDrop);

        for(int i = 0; i < list.size(); i++) {
            Stage st = list.get(i);

            if(((DefStageInfo) st.info).drop != null && ((DefStageInfo) st.info).drop.length != 0) {
                for(int[] drop : ((DefStageInfo) st.info).drop) {
                    if(drop[1] >= 1000 && drop[1] < 30000) {
                        requiredIDs.add(drop[1]);
                    }
                }
            }

            if(((DefStageInfo) st.info).time != null && ((DefStageInfo) st.info).time.length != 0) {
                for(int[] drop : ((DefStageInfo) st.info).time) {
                    if(drop[1] >= 1000 && drop[1] < 30000) {
                        requiredIDs.add(drop[1]);
                    }
                }
            }
        }

        if(dropFile != null) {
            Queue<String> dropLines = dropFile.getData().readLine();

            dropLines.poll();

            while(dropLines.size() > 0) {
                String line = dropLines.poll();

                if(line == null || line.isBlank())
                    break;

                String[] data = line.split(",");

                if(data.length < 3)
                    break;

                if(requiredIDs.contains(StaticStore.safeParseInt(data[0]))) {
                    int egg = getEggValue(StaticStore.safeParseInt(data[2]), unitBuy);

                    rewardToUnitIcon.put(StaticStore.safeParseInt(data[0]), "gatyachara_" + Data.trio(egg == -1 ? StaticStore.safeParseInt(data[2]) : egg) + "_" + (egg == -1 ? "f" : "m") + ".png");
                }
            }
        }

        VFile buyFile = VFile.getFile(unitBuy);

        if(buyFile != null) {
            Queue<String> buyLines = buyFile.getData().readLine();

            while(buyLines.size() > 0) {
                String line = buyLines.poll();

                if(line == null || line.isBlank())
                    break;

                String[] data = line.split(",");

                if(data.length < 24)
                    break;

                if(requiredIDs.contains(StaticStore.safeParseInt(data[23]))) {
                    int egg = -1;

                    int firstEgg = StaticStore.safeParseInt(data[data.length - 2]);
                    int secondEgg = StaticStore.safeParseInt(data[data.length - 1]);

                    if(secondEgg != -1)
                        egg = secondEgg;
                    else if(firstEgg != -1)
                        egg = firstEgg;

                    rewardToUnitIcon.put(StaticStore.safeParseInt(data[23]), "gatyachara_" + Data.trio(egg == -1 ? StaticStore.safeParseInt(data[2]) : egg) + "_" + (egg == -1 ? "f" : "m") + ".png");
                }
            }
        }
    }

    public void handleNewEnemies(File enemyName, File container) {
        if(newEnemies.isEmpty())
            return;

        VFile vf = VFile.getFile(enemyName);

        if(vf != null) {
            Queue<String> qs = vf.getData().readLine();

            int count = 0;

            while(qs.size() > 0) {
                String line = qs.poll();

                if(line == null || line.isBlank())
                    break;

                if(newEnemies.contains(count) && !line.equals("ダミー")) {
                    enemyNames.put(count, line);
                }
            }
        }

        File[] files = container.listFiles();

        if(files == null)
            return;

        List<String> requiredIcons = new ArrayList<>();

        for(int id : newEnemies) {
            requiredIcons.add("enemy_icon_" + Data.trio(id) + ".png");
        }

        for(File f : files) {
            if(requiredIcons.contains(f.getName())) {
                requiredIcons.remove(f.getName());

                enemyIcons.put(CommonStatic.parseIntN(f.getName()), f);
            }
        }
    }

    public void handleNewRewards(File rewardName, File container) {
        if(newRewards.isEmpty())
            return;

        VFile vf = VFile.getFile(rewardName);

        if(vf != null) {
            Queue<String> qs = vf.getData().readLine();

            int count = 0;

            while(qs.size() > 0) {
                String line = qs.poll();

                if(line == null)
                    break;

                String[] data = line.split(",");

                if(data.length < 1)
                    break;

                if(newRewards.contains(count)) {
                    rewardNames.put(count, data[0]);
                }

                count++;
            }
        }

        File[] files = container.listFiles();

        if(files == null)
            return;

        List<String> requiredIcons = new ArrayList<>();

        for(int id : newRewards) {
            requiredIcons.add("gatyaitemD_" + Data.duo(id) + "_f.png");
        }

        for(File f : files) {
            if(requiredIcons.contains(f.getName())) {
                requiredIcons.remove(f.getName());

                rewardIcons.put(CommonStatic.parseIntN(f.getName()), f);
            }
        }
    }

    public void handleNewUnits(File unitDrop, File unitBuy, File textContainer, File imageContainer) {
        if(newUnits.isEmpty())
            return;

        VFile vf = VFile.getFile(unitDrop);

        if(vf != null) {
            Queue<String> qs = vf.getData().readLine();

            qs.poll();

            while(qs.size() > 0) {
                String line = qs.poll();

                if(line == null)
                    break;

                String[] data = line.split(",");

                if(data.length < 3)
                    break;

                int id = StaticStore.safeParseInt(data[0]);

                if(newUnits.contains(id)) {
                    int unit = StaticStore.safeParseInt(data[2]);

                    File[] files = textContainer.listFiles();

                    if(files == null)
                        break;

                    for(File f : files) {
                        if(f.getName().startsWith("Unit_Explanation" + (unit + 1))) {
                            String name = getUnitName(f, 0);

                            if(name != null) {
                                rewardNames.put(id, name);

                                break;
                            }
                        }
                    }

                    files = imageContainer.listFiles();

                    if(files == null)
                        break;

                    int egg = getEggValue(unit, unitBuy);

                    String fileName = "gatyachara_" + Data.trio(egg == -1 ? unit : egg) + "_" + (egg == -1 ? "f" : "m") + ".png";

                    for(File f : files) {
                        if(f.getName().equals(fileName)) {
                            unitIcons.put(id, f);

                            break;
                        }
                    }
                }
            }
        }
    }

    public void handleNewGroups(File container) {
        if(newUnits.isEmpty())
            return;

        for(int unit : newGroups) {
            File[] files = container.listFiles();

            if(files == null)
                break;

            for(File f : files) {
                if(f.getName().startsWith("Unit_Explanation" + unit)) {
                    String name = getUnitName(f, 0);

                    if(name != null) {
                        unitNames.put(unit, name);

                        break;
                    }
                }
            }
        }
    }

    public void handleNewTrueForms(File unitBuy, File textContainer, File imageContainer) {
        if(newTrueForms.isEmpty())
            return;

        VFile vf = VFile.getFile(unitBuy);

        if(vf != null) {
            Queue<String> qs = vf.getData().readLine();

            int count = 0;

            while(qs.size() > 0) {
                String line = qs.poll();

                if(line == null || line.isBlank())
                    break;

                String[] data = line.split(",");

                if(data.length < 24) {
                    count++;

                    continue;
                }

                int id = StaticStore.safeParseInt(data[23]);

                if(newTrueForms.contains(id)) {
                    File[] files = textContainer.listFiles();

                    if(files == null)
                        break;

                    for(File f : files) {
                        if(f.getName().startsWith("Unit_Explanation" + count)) {
                            String name = getUnitName(f, 2);

                            if(name != null) {
                                rewardNames.put(id, name);

                                break;
                            }
                        }
                    }

                    files = imageContainer.listFiles();

                    if(files == null)
                        break;

                    int egg = getEggValue(count, unitBuy);

                    String fileName = "gatyachara_" + Data.trio(egg == -1 ? id : egg) + "_" + (egg == -1 ? "f" : "m") + ".png";

                    for(File f : files) {
                        if(f.getName().equals(fileName)) {
                            trueFormIcons.put(CommonStatic.parseIntN(f.getName()), f);

                            break;
                        }
                    }
                }
            }
        }


    }

    private String getUnitName(File text, int form) {
        VFile vf = VFile.getFile(text);

        if(vf == null)
            return null;

        Queue<String> units = vf.getData().readLine();

        int count = 0;

        while(units.size() > 0) {
            String unitLine = units.poll();

            if(unitLine == null)
                return null;

            if(count == form) {
                String[] unitData = unitLine.split(",");

                if(unitData.length < 1)
                    return null;

                return unitData[0];
            } else {
                count++;
            }
        }

        return null;
    }

    private int getEggValue(int uid, File unitBuy) {
        try {
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
        } catch (IOException e) {
            StaticStore.logger.uploadErrorLog(e, "E/CustomStageMap::getEggValue - Failed to get egg value from file");
        }

        return -1;
    }
}
